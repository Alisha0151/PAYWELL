import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/network/api_client.dart';
import '../core/network/api_exception.dart';
import '../core/network/token_storage.dart';
import 'models.dart';
import 'repositories.dart';

// ---- infrastructure
final tokenStorageProvider = Provider<TokenStorage>((_) => SecureTokenStorage());

final apiClientProvider = Provider<ApiClient>((ref) => ApiClient(
      storage: ref.watch(tokenStorageProvider),
      onUnauthorized: () => ref.read(authProvider.notifier).forceLogout(),
    ));

final authRepositoryProvider = Provider((ref) => AuthRepository(ref.watch(apiClientProvider)));
final walletRepositoryProvider = Provider((ref) => WalletRepository(ref.watch(apiClientProvider)));
final paymentRepositoryProvider = Provider((ref) => PaymentRepository(ref.watch(apiClientProvider)));
final profileRepositoryProvider = Provider((ref) => ProfileRepository(ref.watch(apiClientProvider)));
final insightsRepositoryProvider = Provider((ref) => InsightsRepository(ref.watch(apiClientProvider)));

// ---- app flow flags
final splashDoneProvider = StateProvider<bool>((_) => false);
final onboardedProvider = StateProvider<bool>((_) => false);

// ---- auth
enum AuthStatus { unknown, unauthenticated, authenticated }

class AuthState {
  const AuthState(this.status, {this.user});
  final AuthStatus status;
  final AppUser? user;
}

class AuthNotifier extends Notifier<AuthState> {
  @override
  AuthState build() {
    Future<void>.microtask(_bootstrap);
    return const AuthState(AuthStatus.unknown);
  }

  Future<void> _bootstrap() async {
    final storage = ref.read(tokenStorageProvider);
    ref.read(onboardedProvider.notifier).state = await storage.readOnboarded();
    final token = await storage.readAccess();
    if (token == null) {
      state = const AuthState(AuthStatus.unauthenticated);
      return;
    }
    try {
      final u = await ref.read(authRepositoryProvider).me();
      state = AuthState(AuthStatus.authenticated, user: u);
    } on ApiException catch (e) {
      // Only a rejected session logs out; a flaky network keeps the stored session.
      state = e.isUnauthorized ? const AuthState(AuthStatus.unauthenticated) : const AuthState(AuthStatus.authenticated);
    }
  }

  Future<void> _adopt(Session s) async {
    await ref.read(tokenStorageProvider).saveTokens(access: s.accessToken, refresh: s.refreshToken);
    state = AuthState(AuthStatus.authenticated, user: s.user);
    if (s.user == null) {
      try {
        state = AuthState(AuthStatus.authenticated, user: await ref.read(authRepositoryProvider).me());
      } catch (_) {}
    }
  }

  Future<void> login(String email, String password) async => _adopt(await ref.read(authRepositoryProvider).login(email, password));

  Future<void> signup({required String fullName, required String email, required String phone, required String password}) async =>
      _adopt(await ref.read(authRepositoryProvider).signup(fullName: fullName, email: email, phone: phone, password: password));

  Future<void> logout() async {
    await ref.read(authRepositoryProvider).logout();
    await forceLogout();
  }

  Future<void> forceLogout() async {
    await ref.read(tokenStorageProvider).clear();
    state = const AuthState(AuthStatus.unauthenticated);
    ref.invalidate(walletProvider);
    ref.invalidate(recentTxnsProvider);
  }
}

final authProvider = NotifierProvider<AuthNotifier, AuthState>(AuthNotifier.new);

// ---- data
final profileProvider = FutureProvider.autoDispose<ProfileSettings>((ref) => ref.watch(profileRepositoryProvider).get());
final walletProvider = FutureProvider.autoDispose<Wallet>((ref) => ref.watch(walletRepositoryProvider).wallet());
final recentTxnsProvider =
    FutureProvider.autoDispose<List<Txn>>((ref) async => (await ref.watch(walletRepositoryProvider).transactions(size: 5)).items);
final radarProvider = FutureProvider.autoDispose<RadarSummary>((ref) => ref.watch(insightsRepositoryProvider).radar());
final rewardsProvider = FutureProvider.autoDispose<RewardsSummary>((ref) => ref.watch(insightsRepositoryProvider).rewards());
final insightsProvider = FutureProvider.autoDispose<InsightsSummary>((ref) => ref.watch(insightsRepositoryProvider).insights());

class HistoryState {
  const HistoryState({this.items = const [], this.page = -1, this.last = false, this.loading = false, this.error});
  final List<Txn> items;
  final int page;
  final bool last, loading;
  final String? error;
  HistoryState copy({List<Txn>? items, int? page, bool? last, bool? loading, String? error, bool clearError = false}) => HistoryState(
      items: items ?? this.items, page: page ?? this.page, last: last ?? this.last, loading: loading ?? this.loading,
      error: clearError ? null : (error ?? this.error));
}

class HistoryNotifier extends AutoDisposeNotifier<HistoryState> {
  @override
  HistoryState build() {
    Future<void>.microtask(loadMore);
    return const HistoryState();
  }

  Future<void> loadMore() async {
    if (state.loading || state.last) return;
    state = state.copy(loading: true, clearError: true);
    try {
      final r = await ref.read(walletRepositoryProvider).transactions(page: state.page + 1);
      state = state.copy(items: [...state.items, ...r.items], page: state.page + 1, last: r.last || r.items.isEmpty, loading: false);
    } on ApiException catch (e) {
      state = state.copy(loading: false, error: e.message);
    }
  }

  Future<void> refresh() async {
    state = const HistoryState();
    await loadMore();
  }
}

final historyProvider = NotifierProvider.autoDispose<HistoryNotifier, HistoryState>(HistoryNotifier.new);

/// Call after any money-moving action so balances/history refetch.
void refreshMoney(WidgetRef ref) {
  ref.invalidate(walletProvider);
  ref.invalidate(recentTxnsProvider);
  ref.invalidate(historyProvider);
  ref.invalidate(radarProvider);
  ref.invalidate(rewardsProvider);
  ref.invalidate(insightsProvider);
}
