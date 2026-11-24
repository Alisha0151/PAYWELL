import '../core/format/money.dart';
import '../core/network/api_client.dart';
import 'models.dart';

Map<String, dynamic> _m(dynamic d) => d is Map ? Map<String, dynamic>.from(d) : <String, dynamic>{};

class AuthRepository {
  AuthRepository(this.api);
  final ApiClient api;

  Future<Session> login(String email, String password) async =>
      Session.fromJson(_m(await api.post('/api/v1/auth/login', body: {'email': email, 'password': password})));

  Future<Session> signup({required String fullName, required String email, required String phone, required String password}) async =>
      Session.fromJson(_m(await api.post('/api/v1/auth/signup', body: {'fullName': fullName, 'email': email, if (phone.isNotEmpty) 'phone': phone, 'password': password})));

  Future<AppUser> me() async => AppUser.fromJson(_m(await api.get('/api/v1/profile')));

  Future<void> forgotPassword(String email) => api.post('/api/v1/auth/forgot-password', body: {'email': email});

  Future<void> resetPassword({required String email, required String code, required String newPassword}) =>
      api.post('/api/v1/auth/reset-password', body: {'email': email, 'code': code, 'newPassword': newPassword});

  Future<void> logout() async {
    try {
      await api.post('/api/v1/auth/logout');
    } catch (_) {/* best effort */}
  }
}

class WalletRepository {
  WalletRepository(this.api);
  final ApiClient api;

  Future<Wallet> wallet() async => Wallet.fromJson(_m(await api.get('/api/v1/wallet')));

  Future<PageResult<Txn>> transactions({int page = 0, int size = 20}) async =>
      PageResult.txns(await api.get('/api/v1/transactions', query: {'page': page, 'size': size}));

  /// Simulated top-up. Pass the same [key] when retrying the same user action.
  Future<PaymentResult> topUp(Money amount, {String? key}) async => PaymentResult.fromJson(
      _m(await api.postIdempotent('/api/v1/wallet/top-up', body: {'amount': amount.toApiString()}, key: key)));
}

class PaymentRepository {
  PaymentRepository(this.api);
  final ApiClient api;

  Future<PaymentResult> send({required String recipient, required Money amount, String note = '', String? key, bool acknowledgeRisk = false}) async =>
      PaymentResult.fromJson(_m(await api.postIdempotent('/api/v1/payments/send',
          body: {'handle': recipient, 'amount': amount.toApiString(), if (note.isNotEmpty) 'note': note, if (acknowledgeRisk) 'acknowledgeRisk': true}, key: key)));

  Future<PaymentResult> request({required String from, required Money amount, String note = '', String? key}) async =>
      PaymentResult.fromJson(_m(await api.postIdempotent('/api/v1/payments/requests',
          body: {'payerHandle': from, 'amount': amount.toApiString(), if (note.isNotEmpty) 'note': note}, key: key)));
}

class InsightsRepository {
  InsightsRepository(this.api);
  final ApiClient api;
  Future<RadarSummary> radar() async => RadarSummary.fromJson(_m(await api.get('/api/v1/radar')));
  Future<RewardsSummary> rewards() async => RewardsSummary.fromJson(_m(await api.get('/api/v1/rewards')));
  Future<void> redeem(String rewardId, {String? key}) => api.post('/api/v1/rewards/$rewardId/redeem');
  Future<InsightsSummary> insights() async => InsightsSummary.fromJson(_m(await api.get('/api/v1/insights/summary')));
}

class ProfileRepository {
  ProfileRepository(this.api);
  final ApiClient api;
  Future<ProfileSettings> get() async => ProfileSettings.fromJson(_m(await api.get('/api/v1/profile')));
  Future<ProfileSettings> update(Map<String, Object?> changes) async =>
      ProfileSettings.fromJson(_m(await api.patch('/api/v1/profile', body: changes)));
  Future<void> changePassword(String current, String next) =>
      api.post('/api/v1/profile/change-password', body: {'currentPassword': current, 'newPassword': next});
}
