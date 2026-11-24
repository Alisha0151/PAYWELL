import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/format/ids.dart';
import '../../core/format/money.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';

class TopUpScreen extends ConsumerStatefulWidget {
  const TopUpScreen({super.key});
  @override
  ConsumerState<TopUpScreen> createState() => _TopUpScreenState();
}

class _TopUpScreenState extends ConsumerState<TopUpScreen> {
  final _form = GlobalKey<FormState>();
  final _amount = TextEditingController();
  bool _busy = false;
  String _key = newIdempotencyKey();

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await ref.read(walletRepositoryProvider).topUp(Money.tryParseInput(_amount.text)!, key: _key);
      _key = newIdempotencyKey();
      refreshMoney(ref);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Demo funds added (simulated).')));
        context.pop();
      }
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Top up (simulated)')),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Form(
              key: _form,
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                const SimulatedBanner(),
                const SizedBox(height: 12),
                const Text('Adds demo funds to your wallet. No card or bank account is charged.', style: TextStyle(color: Brand.textMuted)),
                const SizedBox(height: 18),
                TextFormField(
                  controller: _amount, keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'Amount', prefixText: '₹ '),
                  validator: (v) => Money.tryParseInput(v ?? '') == null ? 'Enter a valid amount (max 2 decimals)' : null,
                ),
                const SizedBox(height: 16),
                AppButton(label: 'Add demo funds', onPressed: _submit, loading: _busy),
              ]),
            ),
          ),
        ),
      );
}
