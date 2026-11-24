import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/brand/paywell_logo.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  Future<void> _toggle(BuildContext context, WidgetRef ref, String key, bool v) async {
    try {
      await ref.read(profileRepositoryProvider).update({key: v});
      ref.invalidate(profileProvider);
    } catch (e) {
      if (context.mounted) showError(context, e);
    }
  }

  Future<void> _changePassword(BuildContext context, WidgetRef ref) async {
    final cur = TextEditingController(), next = TextEditingController();
    final form = GlobalKey<FormState>();
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Change password'),
        content: Form(
          key: form,
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            TextFormField(controller: cur, obscureText: true, decoration: const InputDecoration(labelText: 'Current password'), validator: (v) => requiredValidator(v, 'Current password')),
            const SizedBox(height: 12),
            TextFormField(controller: next, obscureText: true, decoration: const InputDecoration(labelText: 'New password'), validator: passwordValidator),
          ]),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Cancel')),
          TextButton(onPressed: () { if (form.currentState!.validate()) Navigator.pop(c, true); }, child: const Text('Update')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await ref.read(profileRepositoryProvider).changePassword(cur.text, next.text);
      if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Password updated.')));
    } catch (e) {
      if (context.mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final prof = ref.watch(profileProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(profileProvider),
        child: prof.when(
          loading: () => ListView(children: const [LoadingView()]),
          error: (e, _) => ListView(children: [ErrorView(error: e, onRetry: () => ref.invalidate(profileProvider))]),
          data: (p) => ListView(padding: const EdgeInsets.all(16), physics: const AlwaysScrollableScrollPhysics(), children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(gradient: Brand.heroGradient, borderRadius: BorderRadius.circular(24)),
              child: Row(children: [
                CircleAvatar(radius: 28, backgroundColor: Brand.cyan.withValues(alpha: .2), child: Text(p.fullName.isEmpty ? '?' : p.fullName[0].toUpperCase(), style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w800))),
                const SizedBox(width: 14),
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(p.fullName, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w700)),
                  Text(p.email, style: const TextStyle(color: Colors.white70, fontSize: 13)),
                  if (p.handle.isNotEmpty) Text('PAYWELL ID: ${p.handle}', style: const TextStyle(color: Brand.cyan, fontSize: 13)),
                ])),
              ]),
            ),
            const SectionHeader('Notifications'),
            _Switch('Payments', p.notifyPayments, (v) => _toggle(context, ref, 'notifyPayments', v)),
            _Switch('Rewards', p.notifyRewards, (v) => _toggle(context, ref, 'notifyRewards', v)),
            _Switch('Bills', p.notifyBills, (v) => _toggle(context, ref, 'notifyBills', v)),
            _Switch('Security', p.notifySecurity, (v) => _toggle(context, ref, 'notifySecurity', v)),
            const SectionHeader('Privacy'),
            _Switch('Hide balance by default', p.hideBalanceDefault, (v) => _toggle(context, ref, 'hideBalanceDefault', v)),
            const SectionHeader('Account'),
            ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.lock_outline), title: const Text('Change password'), onTap: () => _changePassword(context, ref)),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.logout, color: Brand.danger),
              title: const Text('Sign out', style: TextStyle(color: Brand.danger)),
              onTap: () => ref.read(authProvider.notifier).logout(),
            ),
            const SectionHeader('About'),
            const Text(
              'PAYWELL is a demonstration product. Payments, balances and rewards are simulated; no real money moves. '
              'PAYWELL is not a bank or a licensed payment service.',
              style: TextStyle(color: Brand.textMuted, fontSize: 13),
            ),
            const SizedBox(height: 20),
            const Center(child: PaywellLogo(width: 200, style: PaywellLogoStyle.crystal)),
          ]),
        ),
      ),
    );
  }
}

class _Switch extends StatelessWidget {
  const _Switch(this.title, this.value, this.onChanged);
  final String title;
  final bool value;
  final ValueChanged<bool> onChanged;
  @override
  Widget build(BuildContext context) => SwitchListTile(contentPadding: EdgeInsets.zero, title: Text(title), value: value, onChanged: onChanged);
}
