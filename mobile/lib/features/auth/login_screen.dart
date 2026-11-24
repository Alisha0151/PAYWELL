import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/brand/paywell_logo.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});
  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _form = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _pw = TextEditingController();
  bool _busy = false, _hide = true;

  @override
  void dispose() {
    _email.dispose();
    _pw.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await ref.read(authProvider.notifier).login(_email.text.trim(), _pw.text);
      // Router redirects to /home once authenticated.
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _form,
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                const SizedBox(height: 8),
                const Center(child: PaywellLogo(width: 280)),
                const SizedBox(height: 24),
                const Text('Welcome back', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800)),
                const SizedBox(height: 6),
                const Text('Sign in to your PAYWELL account.', style: TextStyle(color: Brand.textMuted)),
                const SizedBox(height: 24),
                TextFormField(controller: _email, keyboardType: TextInputType.emailAddress, autofillHints: const [AutofillHints.email],
                    decoration: const InputDecoration(labelText: 'Email'), validator: emailValidator),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _pw, obscureText: _hide, autofillHints: const [AutofillHints.password],
                  decoration: InputDecoration(labelText: 'Password', suffixIcon: IconButton(icon: Icon(_hide ? Icons.visibility_off : Icons.visibility), onPressed: () => setState(() => _hide = !_hide))),
                  validator: (v) => requiredValidator(v, 'Password'),
                  onFieldSubmitted: (_) => _submit(),
                ),
                Align(alignment: Alignment.centerRight, child: TextButton(onPressed: () => context.push('/forgot'), child: const Text('Forgot password?'))),
                const SizedBox(height: 8),
                AppButton(label: 'Sign in', onPressed: _submit, loading: _busy),
                const SizedBox(height: 12),
                OutlinedButton(onPressed: () => context.go('/signup'), child: const Text('Create an account')),
                const SizedBox(height: 24),
                const Text('Demo product: PAYWELL is not a bank and does not move real money.', textAlign: TextAlign.center, style: TextStyle(fontSize: 12, color: Brand.textMuted)),
              ]),
            ),
          ),
        ),
      );
}
