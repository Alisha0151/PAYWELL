import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';
import '../history/txn_tile.dart';

class WalletScreen extends ConsumerWidget {
  const WalletScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final wallet = ref.watch(walletProvider);
    final recent = ref.watch(recentTxnsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Wallet')),
      body: RefreshIndicator(
        onRefresh: () async => refreshMoney(ref),
        child: ListView(padding: const EdgeInsets.all(16), physics: const AlwaysScrollableScrollPhysics(), children: [
          const SimulatedBanner(),
          const SizedBox(height: 14),
          wallet.when(
            data: (w) => Container(
              padding: const EdgeInsets.all(22),
              decoration: BoxDecoration(gradient: Brand.heroGradient, borderRadius: BorderRadius.circular(24)),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Total balance', style: TextStyle(color: Brand.cyan, fontWeight: FontWeight.w600)),
                const SizedBox(height: 6),
                Text(w.balance.format(), style: const TextStyle(color: Colors.white, fontSize: 34, fontWeight: FontWeight.w800)),
                const SizedBox(height: 14),
                Text('Available: ${w.available.format()}', style: const TextStyle(color: Colors.white70)),
                Text('Currency: ${w.currency}', style: const TextStyle(color: Colors.white70)),
              ]),
            ),
            loading: () => const LoadingView(),
            error: (e, _) => ErrorView(error: e, onRetry: () => ref.invalidate(walletProvider)),
          ),
          const SizedBox(height: 16),
          Row(children: [
            Expanded(child: FilledButton.icon(onPressed: () => context.push('/pay/topup'), icon: const Icon(Icons.add), label: const Text('Top up'))),
            const SizedBox(width: 12),
            Expanded(child: OutlinedButton.icon(onPressed: () => context.push('/pay/send'), icon: const Icon(Icons.north_east), label: const Text('Send'))),
          ]),
          SectionHeader('Latest', action: 'History', onAction: () => context.push('/history')),
          recent.when(
            data: (l) => l.isEmpty ? const Padding(padding: EdgeInsets.all(24), child: Text('No transactions yet.', style: TextStyle(color: Brand.textMuted))) : Column(children: [for (final t in l) TxnTile(t)]),
            loading: () => const LoadingView(),
            error: (e, _) => ErrorView(error: e, onRetry: () => ref.invalidate(recentTxnsProvider)),
          ),
        ]),
      ),
    );
  }
}
