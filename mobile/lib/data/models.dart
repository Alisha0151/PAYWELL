import '../core/format/money.dart';

String _s(Object? v, [String d = '']) => v == null ? d : v.toString();
int _i(Object? v, [int d = 0]) => v is num ? v.toInt() : (int.tryParse(_s(v)) ?? d);
List<Map<String, dynamic>> _list(Object? v) =>
    v is List ? v.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList() : <Map<String, dynamic>>[];

class AppUser {
  const AppUser({required this.id, required this.fullName, required this.email, this.phone = '', this.handle = '', this.phoneVerified = false});
  final String id, fullName, email, phone, handle;
  final bool phoneVerified;

  String get firstName => fullName.trim().isEmpty ? 'there' : fullName.trim().split(' ').first;

  factory AppUser.fromJson(Map<String, dynamic> j) => AppUser(
        id: _s(j['id']),
        fullName: _s(j['fullName'] ?? j['name']),
        email: _s(j['email']),
        phone: _s(j['phone']),
        handle: _s(j['handle'] ?? j['paywellId']),
        phoneVerified: j['phoneVerified'] == true,
      );
}

class Session {
  const Session({required this.accessToken, this.refreshToken, this.user});
  final String accessToken;
  final String? refreshToken;
  final AppUser? user;

  factory Session.fromJson(Map<String, dynamic> j) => Session(
        accessToken: _s(j['accessToken']),
        refreshToken: j['refreshToken']?.toString(),
        user: j['user'] is Map ? AppUser.fromJson(Map<String, dynamic>.from(j['user'] as Map)) : null,
      );
}

class Wallet {
  const Wallet({required this.id, required this.balance, required this.available, required this.currency});
  final String id;
  final Money balance, available;
  final String currency;

  factory Wallet.fromJson(Map<String, dynamic> j) {
    final c = _s(j['currency'], 'INR');
    final bal = Money.parse(j['balance'], currency: c);
    return Wallet(id: _s(j['id']), balance: bal, available: j['availableBalance'] == null ? bal : Money.parse(j['availableBalance'], currency: c), currency: c);
  }
}

enum TxnType { send, receive, topUp, reward, other }

TxnType _type(String s) {
  switch (s.toUpperCase()) {
    case 'P2P_SEND':
      return TxnType.send;
    case 'P2P_RECEIVE':
      return TxnType.receive;
    case 'TOPUP':
      return TxnType.topUp;
    case 'REWARD':
      return TxnType.reward;
    default:
      return TxnType.other;
  }
}

class Txn {
  const Txn({required this.id, required this.type, required this.status, required this.amount, required this.counterparty, required this.note, required this.createdAt, required this.reference, required this.credit, this.simulated = true});
  final String id, status, counterparty, note, reference;
  final TxnType type;
  final Money amount;
  final DateTime createdAt;
  final bool credit, simulated;

  bool get isCredit => credit;
  Money get signed => Money(credit ? amount.minor.abs() : -amount.minor.abs(), amount.currency);

  factory Txn.fromJson(Map<String, dynamic> j) => Txn(
        id: _s(j['id']),
        type: _type(_s(j['type'])),
        status: _s(j['status'], 'SUCCESS'),
        amount: Money.parse(j['amount'], currency: _s(j['currency'], 'INR')),
        counterparty: _s(j['counterpartyName'] ?? j['counterpartyHandle']),
        note: _s(j['note']),
        createdAt: DateTime.tryParse(_s(j['createdAt'])) ?? DateTime.fromMillisecondsSinceEpoch(0),
        reference: _s(j['reference'] ?? j['id']),
        credit: _s(j['direction']).toUpperCase() == 'CREDIT',
        simulated: j['simulated'] != false,
      );
}

class PageResult<T> {
  const PageResult({required this.items, required this.page, required this.last});
  final List<T> items;
  final int page;
  final bool last;

  static PageResult<Txn> txns(dynamic data) {
    if (data is Map) {
      final items = _list(data['content'] ?? data['items']).map(Txn.fromJson).toList();
      final page = _i(data['page'] ?? data['number']);
      final totalPages = _i(data['totalPages'], page + 1);
      return PageResult(items: items, page: page, last: data['last'] == true || page + 1 >= totalPages);
    }
    return const PageResult(items: [], page: 0, last: true);
  }
}

enum Severity { info, warning, critical }

Severity _sev(String s) {
  final u = s.toUpperCase();
  if (u == 'CRITICAL' || u == 'HIGH') return Severity.critical;
  if (u == 'WARNING' || u == 'MEDIUM' || u == 'WARN') return Severity.warning;
  return Severity.info;
}

class CategoryShare {
  const CategoryShare({required this.name, required this.amount, required this.count, required this.sharePercent});
  final String name, sharePercent;
  final Money amount;
  final int count;
  factory CategoryShare.fromJson(Map<String, dynamic> j) => CategoryShare(
      name: _s(j['category']), amount: Money.parse(j['amount']), count: _i(j['count']), sharePercent: _s(j['sharePercent'], '0'));
}

class RadarAnomaly {
  const RadarAnomaly({required this.kind, required this.severity, required this.message, required this.amount});
  final String kind, message;
  final Severity severity;
  final Money amount;
  factory RadarAnomaly.fromJson(Map<String, dynamic> j) => RadarAnomaly(
      kind: _s(j['kind']), message: _s(j['message']), severity: _sev(_s(j['severity'])), amount: Money.parse(j['amount']));
}

class RadarSubscription {
  const RadarSubscription({required this.name, required this.amount, required this.frequency, required this.nextExpected});
  final String name, frequency, nextExpected;
  final Money amount;
  factory RadarSubscription.fromJson(Map<String, dynamic> j) => RadarSubscription(
      name: _s(j['name']), amount: Money.parse(j['amount']), frequency: _s(j['frequency']), nextExpected: _s(j['nextExpected']));
}

class BudgetStatus {
  const BudgetStatus({required this.category, required this.limit, required this.spent, required this.percentUsed, required this.state});
  final String category, state;
  final Money limit, spent;
  final double percentUsed;
  factory BudgetStatus.fromJson(Map<String, dynamic> j) => BudgetStatus(
      category: _s(j['category']), limit: Money.parse(j['limit']), spent: Money.parse(j['spent']),
      percentUsed: double.tryParse(_s(j['percentUsed'])) ?? 0, state: _s(j['state']));
}

class RadarSummary {
  const RadarSummary({required this.totalSpent, required this.totalReceived, required this.net, required this.categories, required this.anomalies, required this.subscriptions, required this.budgets, required this.notice});
  final Money totalSpent, totalReceived, net;
  final List<CategoryShare> categories;
  final List<RadarAnomaly> anomalies;
  final List<RadarSubscription> subscriptions;
  final List<BudgetStatus> budgets;
  final String notice;

  int get alertCount => anomalies.length;
  String get headline => anomalies.isEmpty
      ? 'No unusual activity found in your recent payments.'
      : '${anomalies.length} unusual ${anomalies.length == 1 ? 'item' : 'items'} to review.';

  factory RadarSummary.fromJson(Map<String, dynamic> j) => RadarSummary(
        totalSpent: Money.parse(j['totalSpent']),
        totalReceived: Money.parse(j['totalReceived']),
        net: Money.parse(j['net']),
        categories: _list(j['categories']).map(CategoryShare.fromJson).toList(),
        anomalies: _list(j['anomalies']).map(RadarAnomaly.fromJson).toList(),
        subscriptions: _list(j['subscriptions']).map(RadarSubscription.fromJson).toList(),
        budgets: _list(j['budgets']).map(BudgetStatus.fromJson).toList(),
        notice: _s(j['notice']),
      );
}

class RewardItem {
  const RewardItem({required this.id, required this.title, required this.description, required this.amount, required this.status, required this.kind});
  final String id, title, description, status, kind;
  final Money amount;
  bool get redeemable => status.toUpperCase() == 'AVAILABLE';
  factory RewardItem.fromJson(Map<String, dynamic> j) => RewardItem(
      id: _s(j['id']), title: _s(j['title']), description: _s(j['description']), amount: Money.parse(j['amount']),
      status: _s(j['status']), kind: _s(j['kind']));
}

class RewardsSummary {
  const RewardsSummary({required this.availableTotal, required this.redeemedTotal, required this.items, required this.notice});
  final Money availableTotal, redeemedTotal;
  final List<RewardItem> items;
  final String notice;
  factory RewardsSummary.fromJson(Map<String, dynamic> j) => RewardsSummary(
        availableTotal: Money.parse(j['availableTotal']), redeemedTotal: Money.parse(j['redeemedTotal']),
        items: _list(j['rewards']).map(RewardItem.fromJson).toList(), notice: _s(j['notice']),
      );
}

class MonthPoint {
  const MonthPoint({required this.month, required this.income, required this.spend});
  final String month;
  final Money income, spend;
  factory MonthPoint.fromJson(Map<String, dynamic> j) =>
      MonthPoint(month: _s(j['month']), income: Money.parse(j['income']), spend: Money.parse(j['spend']));
}

class InsightsSummary {
  const InsightsSummary({required this.months, required this.topCategories, required this.savingsRate, required this.avgDaily, required this.changeVsLast, required this.notice});
  final List<MonthPoint> months;
  final List<CategoryShare> topCategories;
  final String savingsRate, changeVsLast, notice;
  final Money avgDaily;
  factory InsightsSummary.fromJson(Map<String, dynamic> j) => InsightsSummary(
        months: _list(j['months']).map(MonthPoint.fromJson).toList(),
        topCategories: _list(j['topCategories']).map((e) => CategoryShare(
            name: _s(e['category']), amount: Money.parse(e['amount']), count: 0, sharePercent: _s(e['sharePercent'], '0'))).toList(),
        savingsRate: _s(j['savingsRatePercent'], '0'),
        avgDaily: Money.parse(j['averageDailySpendThisMonth']),
        changeVsLast: _s(j['spendChangeVsLastMonthPercent'], '0'),
        notice: _s(j['notice']),
      );
}

class ProfileSettings {
  const ProfileSettings({required this.fullName, required this.email, required this.phone, required this.handle, required this.language,
      required this.notifyPayments, required this.notifyRewards, required this.notifyBills, required this.notifySecurity, required this.hideBalanceDefault});
  final String fullName, email, phone, handle, language;
  final bool notifyPayments, notifyRewards, notifyBills, notifySecurity, hideBalanceDefault;
  factory ProfileSettings.fromJson(Map<String, dynamic> j) => ProfileSettings(
        fullName: _s(j['fullName']), email: _s(j['email']), phone: _s(j['phone']), handle: _s(j['handle']), language: _s(j['language'], 'en'),
        notifyPayments: j['notifyPayments'] != false, notifyRewards: j['notifyRewards'] != false,
        notifyBills: j['notifyBills'] != false, notifySecurity: j['notifySecurity'] != false, hideBalanceDefault: j['hideBalanceDefault'] == true,
      );
}

class PaymentResult {
  const PaymentResult({required this.reference, required this.status, required this.simulated, this.riskNote = ''});
  final String reference, status, riskNote;
  final bool simulated;
  factory PaymentResult.fromJson(Map<String, dynamic> j) {
    // Payments return {transaction: {...}, risk, notice}; top-ups and requests return the bare object.
    final t = j['transaction'] is Map ? Map<String, dynamic>.from(j['transaction'] as Map) : j;
    return PaymentResult(
        reference: _s(t['reference'] ?? t['id']), status: _s(t['status'], 'SUCCESS'), simulated: t['simulated'] != false,
        riskNote: _s(j['notice']));
  }
}
