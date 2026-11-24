import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/brand/paywell_logo.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';

class SignupScreen extends ConsumerStatefulWidget {
  const SignupScreen({super.key});
  @override
  ConsumerState<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends ConsumerState<SignupScreen> {
  final _form = GlobalKey<FormState>();
  final _name = TextEditingController(), _email = TextEditingController(), _phone = TextEditingController(), _pw = TextEditingController();
  bool _busy = false, _hide = true, _agree = false;

  @override
  void dispose() {
    for (final c in [_name, _email, _phone, _pw]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    if (!_agree) {
      showError(context, 'Please accept the demo terms to continue.');
      return;
    }
    setState(() => _busy = true);
    try {
      await ref.read(authProvider.notifier).signup(fullName: _name.text.trim(), email: _email.text.trim(), phone: _phone.text.trim(), password: _pw.text);
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(leading: BackButton(onPressed: () => context.go('/login'))),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
            child: Form(
              key: _form,
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                const Center(child: PaywellLogo(width: 220)),
                const SizedBox(height: 16),
                const Text('Create your account', style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800)),
                const SizedBox(height: 20),
                TextFormField(controller: _name, textCapitalization: TextCapitalization.words, decoration: const InputDecoration(labelText: 'Full name'), validator: (v) => requiredValidator(v, 'Name')),
                const SizedBox(height: 14),
                TextFormField(controller: _email, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Email'), validator: emailValidator),
                const SizedBox(height: 14),
                TextFormField(controller: _phone, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Mobile number'),
                    validator: (v) => RegExp(r'^\+?\d{8,15}$').hasMatch((v ?? '').replaceAll(' ', '')) ? null : 'Enter a valid mobile number'),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _pw, obscureText: _hide,
                  decoration: InputDecoration(labelText: 'Password (10+ chars, letter and digit)', suffixIcon: IconButton(icon: Icon(_hide ? Icons.visibility_off : Icons.visibility), onPressed: () => setState(() => _hide = !_hide))),
                  validator: passwordValidator,
                ),
                const SizedBox(height: 8),
                CheckboxListTile(
                  value: _agree, onChanged: (v) => setState(() => _agree = v ?? false), contentPadding: EdgeInsets.zero, controlAffinity: ListTileControlAffinity.leading,
                  title: const Text('I understand PAYWELL is a demo: payments are simulated and no real funds are held.', style: TextStyle(fontSize: 13, color: Brand.textMuted)),
                ),
                const SizedBox(height: 8),
                AppButton(label: 'Create account', onPressed: _submit, loading: _busy),
              ]),
            ),
          ),
        ),
      );
}
