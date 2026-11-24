import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/format/ids.dart';
import '../../core/format/money.dart';
import '../../core/network/api_exception.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/models.dart';
import '../../data/providers.dart';

enum PayMode { send, request }

class SendRequestScreen extends ConsumerStatefulWidget {
  const SendRequestScreen({super.key, required this.mode});
  final PayMode mode;
  @override
  ConsumerState<SendRequestScreen> createState() => _SendRequestScreenState();
}

class _SendRequestScreenState extends ConsumerState<SendRequestScreen> {
  final _form = GlobalKey<FormState>();
  final _who = TextEditingController(), _amount = TextEditingController(), _note = TextEditingController();
  bool _busy = false;
  // One key per user intent: a retry after a network failure re-sends the SAME key, so the server cannot double-apply.
  String _key = newIdempotencyKey();

  bool get _send => widget.mode == PayMode.send;

  @override
  void dispose() {
    _who.dispose();
    _amount.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    final amount = Money.tryParseInput(_amount.text)!;
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text(_send ? 'Confirm simulated payment' : 'Confirm request'),
        content: Text('${_send ? 'Send' : 'Request'} ${amount.format()} ${_send ? 'to' : 'from'} ${_who.text.trim()}?\n\nSimulated – no real money moves.'),
        actions: [TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Cancel')), TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Confirm'))],
      ),
    );
    if (ok != true || !mounted) return;
    setState(() => _busy = true);
    try {
      final repo = ref.read(paymentRepositoryProvider);
      Future<PaymentResult> doSend({bool ack = false}) => repo.send(recipient: _who.text.trim(), amount: amount, note: _note.text.trim(), key: _key, acknowledgeRisk: ack);
      PaymentResult r;
      if (_send) {
        try {
          r = await doSend();
        } on ApiException catch (e) {
          if (e.statusCode != 428) rethrow;
          // Step-up: the risk check wants an explicit confirmation before a simulated payment proceeds.
          if (!mounted) return;
          final go = await showDialog<bool>(
            context: context,
            builder: (c) => AlertDialog(
              title: const Text('Extra check'),
              content: Text('${e.message}\n\nThis is a simulated payment. Continue anyway?'),
              actions: [TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Cancel')), TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Continue'))],
            ),
          );
          if (go != true) return;
          r = await doSend(ack: true);
        }
      } else {
        r = await repo.request(from: _who.text.trim(), amount: amount, note: _note.text.trim(), key: _key);
      }
      _key = newIdempotencyKey(); // success: next submission is a new intent
      refreshMoney(ref);
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (c) => AlertDialog(
          icon: const Icon(Icons.check_circle, color: Brand.ok, size: 40),
          title: Text(_send ? 'Payment recorded' : 'Request created'),
          content: Text('Reference: ${r.reference}\nStatus: ${r.status}\n\nSimulated – no real money moved.'),
          actions: [TextButton(onPressed: () => Navigator.pop(c), child: const Text('Done'))],
        ),
      );
      if (mounted) context.pop();
    } catch (e) {
      if (mounted) showError(context, e); // key kept so a retry is idempotent
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(_send ? 'Send money' : 'Request money')),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Form(
              key: _form,
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                const SimulatedBanner(),
                const SizedBox(height: 18),
                TextFormField(controller: _who, decoration: InputDecoration(labelText: _send ? 'Recipient (PAYWELL ID, email or phone)' : 'Request from (PAYWELL ID, email or phone)'), validator: (v) => requiredValidator(v, 'Recipient')),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _amount, keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'Amount', prefixText: '₹ '),
                  validator: (v) => Money.tryParseInput(v ?? '') == null ? 'Enter a valid amount (max 2 decimals)' : null,
                ),
                const SizedBox(height: 14),
                TextFormField(controller: _note, maxLength: 80, decoration: const InputDecoration(labelText: 'Note (optional)')),
                const SizedBox(height: 8),
                AppButton(label: _send ? 'Review & send' : 'Review & request', onPressed: _submit, loading: _busy),
              ]),
            ),
          ),
        ),
      );
}
