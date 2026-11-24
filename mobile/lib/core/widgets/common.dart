import 'package:flutter/material.dart';

import '../network/api_exception.dart';
import '../theme/app_theme.dart';

/// Shown on every money screen. PAYWELL is a demo: no bank, no licence, no real funds.
class SimulatedBanner extends StatelessWidget {
  const SimulatedBanner({super.key});
  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(color: const Color(0xFFFFF4E5), borderRadius: BorderRadius.circular(12), border: Border.all(color: const Color(0xFFFEC84B))),
        child: const Row(children: [
          Icon(Icons.science_outlined, size: 18, color: Brand.warn),
          SizedBox(width: 10),
          Expanded(child: Text('Simulated – no real money moves', style: TextStyle(color: Brand.warn, fontWeight: FontWeight.w700, fontSize: 13))),
        ]),
      );
}

class LoadingView extends StatelessWidget {
  const LoadingView({super.key});
  @override
  Widget build(BuildContext context) => const Padding(padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()));
}

class ErrorView extends StatelessWidget {
  const ErrorView({super.key, required this.error, this.onRetry});
  final Object error;
  final VoidCallback? onRetry;
  @override
  Widget build(BuildContext context) {
    final msg = error is ApiException ? (error as ApiException).message : 'Something went wrong.';
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Icon(Icons.cloud_off_outlined, size: 40, color: Brand.textMuted),
          const SizedBox(height: 12),
          Text(msg, textAlign: TextAlign.center, style: const TextStyle(color: Brand.textMuted)),
          if (onRetry != null) ...[const SizedBox(height: 12), OutlinedButton(onPressed: onRetry, style: OutlinedButton.styleFrom(minimumSize: const Size(140, 44)), child: const Text('Try again'))],
        ]),
      ),
    );
  }
}

/// Filled button with an inline progress state.
class AppButton extends StatelessWidget {
  const AppButton({super.key, required this.label, required this.onPressed, this.loading = false});
  final String label;
  final VoidCallback? onPressed;
  final bool loading;
  @override
  Widget build(BuildContext context) => FilledButton(
        onPressed: loading ? null : onPressed,
        child: loading ? const SizedBox(height: 22, width: 22, child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white)) : Text(label),
      );
}

class SectionHeader extends StatelessWidget {
  const SectionHeader(this.title, {super.key, this.action, this.onAction});
  final String title;
  final String? action;
  final VoidCallback? onAction;
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 20, bottom: 8),
        child: Row(children: [
          Expanded(child: Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700))),
          if (action != null) TextButton(onPressed: onAction, child: Text(action!)),
        ]),
      );
}

void showError(BuildContext context, Object e) {
  final msg = e is ApiException ? e.message : (e is String ? e : 'Something went wrong. Please try again.');
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(msg), backgroundColor: Brand.danger, behavior: SnackBarBehavior.floating));
}

String? requiredValidator(String? v, [String what = 'This field']) => (v == null || v.trim().isEmpty) ? '$what is required' : null;
String? emailValidator(String? v) => RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch((v ?? '').trim()) ? null : 'Enter a valid email';
String? passwordValidator(String? v) {
  final s = v ?? '';
  if (s.length < 10 || s.length > 72) return '10-72 characters';
  if (!RegExp(r'[A-Za-z]').hasMatch(s) || !RegExp(r'\d').hasMatch(s)) return 'Include a letter and a digit';
  return null;
}
