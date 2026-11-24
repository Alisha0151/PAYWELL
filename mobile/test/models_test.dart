import 'package:flutter_test/flutter_test.dart';
import 'package:paywell/data/models.dart';

void main() {
  test('Txn maps backend enums and direction', () {
    final t = Txn.fromJson({
      'id': 'a', 'reference': 'PW123', 'type': 'P2P_SEND', 'direction': 'DEBIT', 'amount': '250.00', 'currency': 'INR',
      'status': 'SUCCESS', 'counterpartyName': 'Asha', 'createdAt': '2026-11-01T10:00:00Z', 'simulated': true,
    });
    expect(t.type, TxnType.send);
    expect(t.isCredit, isFalse);
    expect(t.signed.minor, -25000);
    expect(t.counterparty, 'Asha');
  });

  test('credit transactions are positive', () {
    final t = Txn.fromJson({'type': 'TOPUP', 'direction': 'CREDIT', 'amount': '10.00'});
    expect(t.type, TxnType.topUp);
    expect(t.signed.minor, 1000);
  });

  test('PageResult reads backend page shape', () {
    final p = PageResult.txns({'items': [{'id': '1', 'type': 'REWARD', 'direction': 'CREDIT', 'amount': '1.00'}], 'page': 0, 'last': true});
    expect(p.items, hasLength(1));
    expect(p.last, isTrue);
  });

  test('PaymentResult unwraps nested transaction', () {
    final r = PaymentResult.fromJson({'transaction': {'reference': 'PW9', 'status': 'SUCCESS', 'simulated': true}, 'notice': 'SIMULATED'});
    expect(r.reference, 'PW9');
    expect(r.simulated, isTrue);
  });

  test('Radar and rewards parse', () {
    final r = RadarSummary.fromJson({'totalSpent': '10.00', 'totalReceived': '5.00', 'net': '-5.00', 'anomalies': [{'kind': 'LARGE', 'severity': 'HIGH', 'message': 'x', 'amount': '9.00'}]});
    expect(r.alertCount, 1);
    expect(r.anomalies.first.severity, Severity.critical);
    final w = RewardsSummary.fromJson({'availableTotal': '2.50', 'redeemedTotal': '0.00', 'rewards': [{'id': 'r', 'status': 'AVAILABLE', 'amount': '2.50'}]});
    expect(w.items.first.redeemable, isTrue);
  });
}
