import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/brand/paywell_logo.dart';
import '../../data/providers.dart';

/// Plays the ~2 s launch sequence, then lets the router redirect (home if signed in, otherwise onboarding/login).
class SplashScreen extends ConsumerWidget {
  const SplashScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
        backgroundColor: const Color(0xFF10266F),
        body: PaywellLaunchLogo(onFinished: () => ref.read(splashDoneProvider.notifier).state = true),
      );
}
