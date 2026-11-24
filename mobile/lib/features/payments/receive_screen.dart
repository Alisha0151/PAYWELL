import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';
import '../../data/providers.dart';

class ReceiveScreen extends ConsumerWidget {
  const ReceiveScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final u = ref.watch(authProvider).user;
    final id = (u?.handle.isNotEmpty ?? false) ? u!.handle : (u?.email ?? '');
    return Scaffold(
      appBar: AppBar(title: const Text('Receive')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        const SimulatedBanner(),
        const SizedBox(height: 20),
        Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(gradient: Brand.heroGradient, borderRadius: BorderRadius.circular(24)),
          child: Column(children: [
            const Icon(Icons.south_west_rounded, color: Brand.cyan, size: 36),
            const SizedBox(height: 12),
            Text(u?.fullName ?? '', style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            SelectableText(id.isEmpty ? 'ID unavailable' : id, style: const TextStyle(color: Brand.cyan, fontSize: 18, fontWeight: FontWeight.w600)),
          ]),
        ),
        const SizedBox(height: 16),
        OutlinedButton.icon(
          onPressed: id.isEmpty
              ? null
              : () async {
                  await Clipboard.setData(ClipboardData(text: id));
                  if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Copied')));
                },
          icon: const Icon(Icons.copy),
          label: const Text('Copy my PAYWELL ID'),
        ),
        const SizedBox(height: 12),
        const Text('Anyone using this demo app can send you simulated funds with this ID.', style: TextStyle(color: Brand.textMuted, fontSize: 13)),
      ]),
    );
  }
}
