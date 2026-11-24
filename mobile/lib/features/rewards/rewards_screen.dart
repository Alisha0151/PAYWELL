import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/models.dart';
import '../../data/providers.dart';

class RewardsScreen extends ConsumerWidget {
  const RewardsScreen({super.key});

  Future<void> _redeem(BuildContext context, WidgetRef ref, RewardItem r) async {
    try {
      await ref.read(insightsRepositoryProvider).redeem(r.id);
      refreshMoney(ref);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${r.amount.format()} added to your simulated wallet.')));
      }
    } catch (e) {
      if (context.mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final rewards = ref.watch(rewardsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Rewards')),
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(rewardsProvider),
        child: rewards.when(
          loading: () => ListView(children: const [LoadingView()]),
          error: (e, _) => ListView(children: [ErrorView(error: e, onRetry: () => ref.invalidate(rewardsProvider))]),
          data: (r) => ListView(padding: const EdgeInsets.all(16), physics: const AlwaysScrollableScrollPhysics(), children: [
            Container(
              padding: const EdgeInsets.all(22),
              decoration: BoxDecoration(gradient: Brand.heroGradient, borderRadius: BorderRadius.circular(24)),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Available rewards', style: TextStyle(color: Brand.cyan, fontWeight: FontWeight.w600)),
                const SizedBox(height: 6),
                Text(r.availableTotal.format(), style: const TextStyle(color: Colors.white, fontSize: 32, fontWeight: FontWeight.w800)),
                const SizedBox(height: 8),
                Text('Redeemed so far: ${r.redeemedTotal.format()}', style: const TextStyle(color: Colors.white70)),
              ]),
            ),
            if (r.notice.isNotEmpty) Padding(padding: const EdgeInsets.only(top: 10), child: Text(r.notice, style: const TextStyle(fontSize: 12, color: Brand.textMuted))),
            const SectionHeader('Your rewards'),
            if (r.items.isEmpty) const Padding(padding: EdgeInsets.all(24), child: Text('No rewards yet. Simulated cashback appears after eligible payments.', style: TextStyle(color: Brand.textMuted))),
            for (final it in r.items)
              Card(
                child: ListTile(
                  contentPadding: const EdgeInsets.all(14),
                  leading: CircleAvatar(backgroundColor: Brand.electric.withValues(alpha: .1), child: const Icon(Icons.card_giftcard, color: Brand.electric)),
                  title: Text(it.title, style: const TextStyle(fontWeight: FontWeight.w700)),
                  subtitle: Text('${it.description}\n${it.amount.format()} · ${it.status.toLowerCase()}'),
                  isThreeLine: true,
                  trailing: it.redeemable ? FilledButton(onPressed: () => _redeem(context, ref, it), style: FilledButton.styleFrom(minimumSize: const Size(92, 40)), child: const Text('Redeem')) : null,
                ),
              ),
          ]),
        ),
      ),
    );
  }
}
