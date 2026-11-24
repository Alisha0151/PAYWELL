import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';

class InsightsScreen extends ConsumerWidget {
  const InsightsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ins = ref.watch(insightsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Insights')),
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(insightsProvider),
        child: ins.when(
          loading: () => ListView(children: const [LoadingView()]),
          error: (e, _) => ListView(children: [ErrorView(error: e, onRetry: () => ref.invalidate(insightsProvider))]),
          data: (s) {
            final maxV = s.months.fold<int>(1, (m, p) => [m, p.income.minor, p.spend.minor].reduce((a, b) => a > b ? a : b));
            return ListView(padding: const EdgeInsets.all(16), physics: const AlwaysScrollableScrollPhysics(), children: [
              Row(children: [
                Expanded(child: _Tile('Savings rate', '${s.savingsRate}%')),
                const SizedBox(width: 12),
                Expanded(child: _Tile('Avg daily spend', s.avgDaily.format())),
              ]),
              const SizedBox(height: 12),
              _Tile('Spend vs last month', '${s.changeVsLast}%'),
              const SectionHeader('Income vs spend'),
              if (s.months.isEmpty) const Text('Not enough activity yet.', style: TextStyle(color: Brand.textMuted)),
              for (final m in s.months)
                Padding(
                  padding: const EdgeInsets.only(bottom: 14),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(m.month, style: const TextStyle(fontWeight: FontWeight.w600)),
                    const SizedBox(height: 6),
                    _Bar(m.income.minor / maxV, Brand.cyan, 'In ${m.income.format()}'),
                    const SizedBox(height: 4),
                    _Bar(m.spend.minor / maxV, Brand.electric, 'Out ${m.spend.format()}'),
                  ]),
                ),
              const SectionHeader('Top categories'),
              for (final c in s.topCategories)
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(c.name.isEmpty ? '' : c.name[0] + c.name.substring(1).toLowerCase()),
                  subtitle: LinearProgressIndicator(value: ((double.tryParse(c.sharePercent) ?? 0) / 100).clamp(0.0, 1.0), minHeight: 6, color: Brand.electric, backgroundColor: Brand.surfaceTint),
                  trailing: Text(c.amount.format(), style: const TextStyle(fontWeight: FontWeight.w700)),
                ),
              if (s.notice.isNotEmpty) Padding(padding: const EdgeInsets.only(top: 12), child: Text(s.notice, style: const TextStyle(fontSize: 12, color: Brand.textMuted))),
            ]);
          },
        ),
      ),
    );
  }
}

class _Tile extends StatelessWidget {
  const _Tile(this.label, this.value);
  final String label, value;
  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(label, style: const TextStyle(color: Brand.textMuted, fontSize: 12)),
            const SizedBox(height: 4),
            Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: Brand.royal)),
          ]),
        ),
      );
}

class _Bar extends StatelessWidget {
  const _Bar(this.frac, this.color, this.label);
  final double frac;
  final Color color;
  final String label;
  @override
  Widget build(BuildContext context) => Row(children: [
        Expanded(
          child: LayoutBuilder(builder: (_, c) => Align(
                alignment: Alignment.centerLeft,
                child: Container(height: 10, width: (c.maxWidth * frac.clamp(0.0, 1.0)).clamp(2.0, c.maxWidth), decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(6))),
              )),
        ),
        const SizedBox(width: 8),
        SizedBox(width: 110, child: Text(label, style: const TextStyle(fontSize: 11, color: Brand.textMuted), textAlign: TextAlign.right)),
      ]);
}
