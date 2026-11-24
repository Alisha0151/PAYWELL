import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';

enum OtpPurpose { reset, verify }

/// OTP entry UI. Whether a code is actually delivered by SMS/email depends on a provider being configured on the server;
/// in local/demo mode nothing is sent and the code is only available from the backend's development log.
class OtpScreen extends ConsumerStatefulWidget {
  const OtpScreen({super.key, required this.email, required this.purpose});
  final String email;
  final OtpPurpose purpose;
  @override
  ConsumerState<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends ConsumerState<OtpScreen> {
  final _form = GlobalKey<FormState>();
  final _code = TextEditingController(), _pw = TextEditingController();
  bool _busy = false;

  @override
  void dispose() {
    _code.dispose();
    _pw.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      final repo = ref.read(authRepositoryProvider);
      if (widget.purpose == OtpPurpose.reset) {
        await repo.resetPassword(email: widget.email, code: _code.text.trim(), newPassword: _pw.text);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Password updated. Please sign in.')));
        context.go('/login');
      } else {
        // The backend only issues codes for password reset; account verification is not part of this build.
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Verification is not enabled in this build.')));
        context.pop();
      }
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final reset = widget.purpose == OtpPurpose.reset;
    return Scaffold(
      appBar: AppBar(title: Text(reset ? 'Enter code' : 'Verify')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _form,
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              Text('Enter the 6-digit code for ${widget.email.isEmpty ? 'your account' : widget.email}.', style: const TextStyle(fontSize: 16)),
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: Brand.surfaceTint, borderRadius: BorderRadius.circular(12)),
                child: const Text(
                  'A code is only delivered if the server has an SMS/email provider configured. In local demo mode nothing is sent; '
                  'the code appears in the backend development log.',
                  style: TextStyle(fontSize: 13, color: Brand.textMuted),
                ),
              ),
              const SizedBox(height: 20),
              TextFormField(
                controller: _code, keyboardType: TextInputType.number, maxLength: 6, textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 28, letterSpacing: 10, fontWeight: FontWeight.w700),
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                decoration: const InputDecoration(counterText: '', hintText: '••••••'),
                validator: (v) => (v ?? '').length == 6 ? null : 'Enter the 6-digit code',
              ),
              if (reset) ...[
                const SizedBox(height: 14),
                TextFormField(controller: _pw, obscureText: true, decoration: const InputDecoration(labelText: 'New password'), validator: passwordValidator),
              ],
              const SizedBox(height: 20),
              AppButton(label: reset ? 'Update password' : 'Verify', onPressed: _submit, loading: _busy),
            ]),
          ),
        ),
      ),
    );
  }
}
