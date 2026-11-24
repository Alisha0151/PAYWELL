import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/brand/paywell_logo.dart';
import '../../core/theme/app_theme.dart';
import '../../data/providers.dart';

class OnboardingScreen extends ConsumerStatefulWidget {
  const OnboardingScreen({super.key});
  @override
  ConsumerState<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends ConsumerState<OnboardingScreen> {
  final _c = PageController();
  int _i = 0;

  static const _pages = [
    (Icons.north_east_rounded, 'Send in a moment', 'Pay friends and family from your PAYWELL wallet with one clear confirmation step.'),
    (Icons.south_west_rounded, 'Receive with ease', 'Share your PAYWELL ID and see incoming payments land in your history.'),
    (Icons.radar_rounded, 'Know your money', 'Money Radar and Insights turn your activity into plain-language signals.'),
  ];

  Future<void> _done(String route) async {
    await ref.read(tokenStorageProvider).setOnboarded();
    ref.read(onboardedProvider.notifier).state = true;
    if (mounted) context.go(route);
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final last = _i == _pages.length - 1;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(children: [
            Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
              const PaywellLogo(width: 56, showWordmark: false),
              TextButton(onPressed: () => _done('/login'), child: const Text('Skip')),
            ]),
            Expanded(
              child: PageView.builder(
                controller: _c,
                itemCount: _pages.length,
                onPageChanged: (i) => setState(() => _i = i),
                itemBuilder: (_, i) {
                  final p = _pages[i];
                  return Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                    Container(
                      width: 140, height: 140,
                      decoration: const BoxDecoration(gradient: Brand.heroGradient, shape: BoxShape.circle),
                      child: Icon(p.$1, size: 64, color: Brand.cyan),
                    ),
                    const SizedBox(height: 32),
                    Text(p.$2, style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w800)),
                    const SizedBox(height: 12),
                    Text(p.$3, textAlign: TextAlign.center, style: const TextStyle(fontSize: 16, color: Brand.textMuted, height: 1.4)),
                  ]);
                },
              ),
            ),
            Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              for (var i = 0; i < _pages.length; i++)
                AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  margin: const EdgeInsets.all(4),
                  width: i == _i ? 24 : 8, height: 8,
                  decoration: BoxDecoration(color: i == _i ? Brand.electric : Brand.electric.withValues(alpha: .2), borderRadius: BorderRadius.circular(4)),
                ),
            ]),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: () => last ? _done('/signup') : _c.nextPage(duration: const Duration(milliseconds: 250), curve: Curves.easeOut),
              child: Text(last ? 'Create account' : 'Next'),
            ),
            if (last) TextButton(onPressed: () => _done('/login'), child: const Text('I already have an account')),
            const SizedBox(height: 8),
            const Text('PAYWELL is a demo product. Payments are simulated.', style: TextStyle(fontSize: 12, color: Brand.textMuted)),
          ]),
        ),
      ),
    );
  }
}
