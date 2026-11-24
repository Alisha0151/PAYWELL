import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/common.dart';
import '../../data/providers.dart';
import 'txn_tile.dart';

class HistoryScreen extends ConsumerStatefulWidget {
  const HistoryScreen({super.key});
  @override
  ConsumerState<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends ConsumerState<HistoryScreen> {
  final _sc = ScrollController();

  @override
  void initState() {
    super.initState();
    _sc.addListener(() {
      if (_sc.position.pixels > _sc.position.maxScrollExtent - 300) ref.read(historyProvider.notifier).loadMore();
    });
  }

  @override
  void dispose() {
    _sc.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final s = ref.watch(historyProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Transaction history')),
      body: RefreshIndicator(
        onRefresh: () => ref.read(historyProvider.notifier).refresh(),
        child: Builder(builder: (_) {
          if (s.items.isEmpty && s.loading) return const LoadingView();
          if (s.items.isEmpty && s.error != null) {
            return ListView(children: [ErrorView(error: s.error!, onRetry: () => ref.read(historyProvider.notifier).refresh())]);
          }
          if (s.items.isEmpty) {
            return ListView(children: const [Padding(padding: EdgeInsets.all(48), child: Center(child: Text('No transactions yet.')))]);
          }
          return ListView.separated(
            controller: _sc,
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            itemCount: s.items.length + 1,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (_, i) {
              if (i < s.items.length) return TxnTile(s.items[i]);
              if (s.loading) return const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator()));
              if (s.error != null) return ErrorView(error: s.error!, onRetry: () => ref.read(historyProvider.notifier).loadMore());
              return const SizedBox(height: 24);
            },
          );
        }),
      ),
    );
  }
}
