import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/theme/app_theme.dart';
import '../../data/models.dart';

class TxnTile extends StatelessWidget {
  const TxnTile(this.t, {super.key});
  final Txn t;

  static IconData icon(TxnType t) => switch (t) {
        TxnType.send => Icons.north_east_rounded,
        TxnType.receive => Icons.south_west_rounded,
        TxnType.topUp => Icons.add_card_rounded,
        TxnType.reward => Icons.card_giftcard,
        TxnType.other => Icons.swap_horiz_rounded,
      };

  @override
  Widget build(BuildContext context) {
    final credit = t.isCredit;
    final title = t.counterparty.isNotEmpty ? t.counterparty : switch (t.type) {
          TxnType.topUp => 'Wallet top-up (simulated)',
          TxnType.reward => 'Reward',
          _ => 'Transaction',
        };
    final status = t.status.toUpperCase();
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
      leading: CircleAvatar(
        backgroundColor: (credit ? Brand.ok : Brand.electric).withValues(alpha: .1),
        child: Icon(icon(t.type), color: credit ? Brand.ok : Brand.electric, size: 20),
      ),
      title: Text(title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w600)),
      subtitle: Text('${DateFormat('d MMM, h:mm a').format(t.createdAt.toLocal())}${status == 'SUCCESS' ? '' : ' · ${status.toLowerCase()}'}',
          style: const TextStyle(fontSize: 12, color: Brand.textMuted)),
      trailing: Text(t.signed.format(signed: true), style: TextStyle(fontWeight: FontWeight.w700, color: credit ? Brand.ok : Brand.text)),
    );
  }
}
