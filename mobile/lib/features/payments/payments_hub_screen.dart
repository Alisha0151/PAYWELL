import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/common.dart';

class PaymentsHubScreen extends StatelessWidget {
  const PaymentsHubScreen({super.key});

  @override
  Widget build(BuildContext context) {
    Widget tile(IconData i, String t, String sub, String route) => Card(
          child: ListTile(
            contentPadding: const EdgeInsets.all(14),
            leading: CircleAvatar(radius: 24, backgroundColor: Brand.electric.withValues(alpha: .1), child: Icon(i, color: Brand.electric)),
            title: Text(t, style: const TextStyle(fontWeight: FontWeight.w700)),
            subtitle: Text(sub),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => context.push(route),
          ),
        );
    return Scaffold(
      appBar: AppBar(title: const Text('Payments')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        const SimulatedBanner(),
        const SizedBox(height: 14),
        tile(Icons.north_east_rounded, 'Send', 'Pay someone by PAYWELL ID, email or phone', '/pay/send'),
        tile(Icons.south_west_rounded, 'Receive', 'Share your PAYWELL ID', '/pay/receive'),
        tile(Icons.request_page_outlined, 'Request', 'Ask someone to pay you', '/pay/request'),
        tile(Icons.add_card_rounded, 'Top up wallet', 'Add simulated demo funds', '/pay/topup'),
        tile(Icons.receipt_long_outlined, 'Transaction history', 'Everything in your wallet', '/history'),
        const SizedBox(height: 8),
        const Text('Payments here are simulated for demonstration. PAYWELL does not hold a banking licence and no real funds are moved or stored.',
            style: TextStyle(fontSize: 12, color: Brand.textMuted)),
      ]),
    );
  }
}
