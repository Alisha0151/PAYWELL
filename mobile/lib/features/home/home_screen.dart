import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/brand/paywell_logo.dart';
import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';
import '../history/txn_tile.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider).user;
    final wallet = ref.watch(walletProvider);
    final recent = ref.watch(recentTxnsProvider);
    final radar = ref.watch(radarProvider);

    return Scaffold(
      appBar: AppBar(
        titleSpacing: 16,
        title: Row(children: [
          const PaywellLogo(width: 44, showWordmark: false),
          const SizedBox(width: 10),
          Text('Hi, ${user?.firstName ?? 'there'}', style: const TextStyle(fontWeight: FontWeight.w700)),
        ]),
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          refreshMoney(ref);
          try {
            await ref.read(walletProvider.future);
          } catch (_) {/* error state is shown by the card */}
        },
        child: ListView(padding: const EdgeInsets.fromLTRB(16, 4, 16, 24), physics: const AlwaysScrollableScrollPhysics(), children: [
          const SimulatedBanner(),
          const SizedBox(height: 14),
          // Deep-royal hero balance card
          Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              gradient: Brand.heroGradient,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [BoxShadow(color: Brand.royal.withValues(alpha: .3), blurRadius: 24, offset: const Offset(0, 10))],
            ),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Wallet balance (simulated)', style: TextStyle(color: Brand.cyan, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              wallet.when(
                data: (w) => Text(w.balance.format(), style: const TextStyle(color: Colors.white, fontSize: 36, fontWeight: FontWeight.w800)),
                loading: () => const SizedBox(height: 44, child: Align(alignment: Alignment.centerLeft, child: CircularProgressIndicator(color: Brand.cyan))),
                error: (e, _) => Row(children: [
                  const Expanded(child: Text('Balance unavailable', style: TextStyle(color: Colors.white70, fontSize: 18))),
                  TextButton(onPressed: () => ref.invalidate(walletProvider), child: const Text('Retry', style: TextStyle(color: Brand.cyan))),
                ]),
              ),
              const SizedBox(height: 18),
              Row(children: [
                _Action(Icons.north_east_rounded, 'Send', () => context.push('/pay/send')),
                _Action(Icons.south_west_rounded, 'Receive', () => context.push('/pay/receive')),
                _Action(Icons.add_rounded, 'Top up', () => context.push('/pay/topup')),
                _Action(Icons.account_balance_wallet_outlined, 'Wallet', () => context.push('/wallet')),
              ]),
            ]),
          ),
          const SectionHeader('Money Radar'),
          Card(
            child: InkWell(
              borderRadius: BorderRadius.circular(18),
              onTap: () => context.push('/radar'),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: radar.when(
                  data: (r) => Row(children: [
                    CircleAvatar(radius: 26, backgroundColor: Brand.electric.withValues(alpha: .1), child: Text('${r.alertCount}', style: const TextStyle(fontWeight: FontWeight.w800, color: Brand.electric, fontSize: 18))),
                    const SizedBox(width: 14),
                    Expanded(child: Text(r.headline, style: const TextStyle(fontWeight: FontWeight.w600))),
                    const Icon(Icons.chevron_right),
                  ]),
                  loading: () => const SizedBox(height: 52, child: Center(child: CircularProgressIndicator())),
                  error: (e, _) => Row(children: [const Expanded(child: Text('Money Radar is unavailable right now.')), TextButton(onPressed: () => ref.invalidate(radarProvider), child: const Text('Retry'))]),
                ),
              ),
            ),
          ),
          SectionHeader('Recent activity', action: 'See all', onAction: () => context.push('/history')),
          recent.when(
            data: (list) => list.isEmpty
                ? const Padding(padding: EdgeInsets.all(24), child: Center(child: Text('No activity yet. Try a simulated top-up.', style: TextStyle(color: Brand.textMuted))))
                : Column(children: [for (final t in list) TxnTile(t)]),
            loading: () => const LoadingView(),
            error: (e, _) => ErrorView(error: e, onRetry: () => ref.invalidate(recentTxnsProvider)),
          ),
        ]),
      ),
    );
  }
}

class _Action extends StatelessWidget {
  const _Action(this.icon, this.label, this.onTap);
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Expanded(
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: Column(children: [
              Container(width: 46, height: 46, decoration: BoxDecoration(color: Colors.white.withValues(alpha: .14), shape: BoxShape.circle), child: Icon(icon, color: Colors.white)),
              const SizedBox(height: 6),
              Text(label, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
            ]),
          ),
        ),
      );
}
