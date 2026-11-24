import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/models.dart';
import '../../data/providers.dart';

class RadarScreen extends ConsumerWidget {
  const RadarScreen({super.key});

  static Color _sev(Severity s) => switch (s) { Severity.critical => Brand.danger, Severity.warning => Brand.warn, Severity.info => Brand.electric };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final radar = ref.watch(radarProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Money Radar')),
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(radarProvider),
        child: radar.when(
          loading: () => ListView(children: const [LoadingView()]),
          error: (e, _) => ListView(children: [ErrorView(error: e, onRetry: () => ref.invalidate(radarProvider))]),
          data: (r) => ListView(padding: const EdgeInsets.all(16), physics: const AlwaysScrollableScrollPhysics(), children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(gradient: Brand.heroGradient, borderRadius: BorderRadius.circular(24)),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Last 30 days', style: TextStyle(color: Brand.cyan, fontWeight: FontWeight.w600)),
                const SizedBox(height: 6),
                Text(r.headline, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w700)),
                const SizedBox(height: 14),
                Row(children: [
                  Expanded(child: _Stat('Spent', r.totalSpent.format())),
                  Expanded(child: _Stat('Received', r.totalReceived.format())),
                  Expanded(child: _Stat('Net', r.net.format(signed: true))),
                ]),
              ]),
            ),
            if (r.notice.isNotEmpty) Padding(padding: const EdgeInsets.only(top: 10), child: Text(r.notice, style: const TextStyle(fontSize: 12, color: Brand.textMuted))),
            const SectionHeader('Needs a look'),
            if (r.anomalies.isEmpty)
              const Text('Nothing unusual detected.', style: TextStyle(color: Brand.textMuted))
            else
              for (final a in r.anomalies)
                Card(child: ListTile(
                  leading: Icon(Icons.radar, color: _sev(a.severity)),
                  title: Text(a.message, style: const TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: Text('${a.kind.replaceAll('_', ' ').toLowerCase()} · ${a.amount.format()}'),
                )),
            const SectionHeader('Where it went'),
            if (r.categories.isEmpty) const Text('No spending yet.', style: TextStyle(color: Brand.textMuted)),
            for (final c in r.categories) _CategoryRow(c),
            const SectionHeader('Recurring payments'),
            if (r.subscriptions.isEmpty) const Text('No recurring payments detected yet.', style: TextStyle(color: Brand.textMuted)),
            for (final s in r.subscriptions)
              ListTile(
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.autorenew, color: Brand.electric),
                title: Text(s.name),
                subtitle: Text('${s.frequency.toLowerCase()} · next ${s.nextExpected}'),
                trailing: Text(s.amount.format(), style: const TextStyle(fontWeight: FontWeight.w700)),
              ),
            if (r.budgets.isNotEmpty) const SectionHeader('Budgets'),
            for (final b in r.budgets)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [Expanded(child: Text(b.category)), Text('${b.spent.format()} / ${b.limit.format()}', style: const TextStyle(color: Brand.textMuted, fontSize: 12))]),
                  const SizedBox(height: 6),
                  LinearProgressIndicator(value: (b.percentUsed / 100).clamp(0.0, 1.0), minHeight: 8, borderRadius: BorderRadius.circular(8),
                      color: b.percentUsed >= 100 ? Brand.danger : Brand.electric, backgroundColor: Brand.surfaceTint),
                ]),
              ),
          ]),
        ),
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat(this.label, this.value);
  final String label, value;
  @override
  Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(label, style: const TextStyle(color: Colors.white70, fontSize: 12)),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700), maxLines: 1, overflow: TextOverflow.ellipsis),
      ]);
}

class _CategoryRow extends StatelessWidget {
  const _CategoryRow(this.c);
  final CategoryShare c;
  @override
  Widget build(BuildContext context) {
    final pct = (double.tryParse(c.sharePercent) ?? 0) / 100;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [Expanded(child: Text(c.name[0] + c.name.substring(1).toLowerCase())), Text(c.amount.format(), style: const TextStyle(fontWeight: FontWeight.w700))]),
        const SizedBox(height: 6),
        LinearProgressIndicator(value: pct.clamp(0.0, 1.0), minHeight: 8, borderRadius: BorderRadius.circular(8), color: Brand.electric, backgroundColor: Brand.surfaceTint),
      ]),
    );
  }
}
