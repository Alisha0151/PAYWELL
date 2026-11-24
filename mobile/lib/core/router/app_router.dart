import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../data/providers.dart';
import '../../features/auth/forgot_password_screen.dart';
import '../../features/auth/login_screen.dart';
import '../../features/auth/otp_screen.dart';
import '../../features/auth/signup_screen.dart';
import '../../features/history/history_screen.dart';
import '../../features/home/home_screen.dart';
import '../../features/insights/insights_screen.dart';
import '../../features/onboarding/onboarding_screen.dart';
import '../../features/payments/payments_hub_screen.dart';
import '../../features/payments/receive_screen.dart';
import '../../features/payments/send_request_screen.dart';
import '../../features/payments/topup_screen.dart';
import '../../features/profile/profile_screen.dart';
import '../../features/radar/radar_screen.dart';
import '../../features/rewards/rewards_screen.dart';
import '../../features/shell/main_shell.dart';
import '../../features/splash/splash_screen.dart';
import '../../features/wallet/wallet_screen.dart';

const publicRoutes = {'/onboarding', '/login', '/signup', '/forgot', '/otp'};

/// Pure redirect rule (unit-tested). Returns null to stay, or the path to go to.
String? redirectFor({required AuthStatus status, required bool splashDone, required bool onboarded, required String location}) {
  final path = Uri.parse(location).path;
  if (!splashDone || status == AuthStatus.unknown) return path == '/splash' ? null : '/splash';
  if (status == AuthStatus.authenticated) {
    return (path == '/splash' || publicRoutes.contains(path)) ? '/home' : null;
  }
  if (path == '/splash') return onboarded ? '/login' : '/onboarding';
  return publicRoutes.contains(path) ? null : '/login';
}

class _RouterRefresh extends ChangeNotifier {
  void poke() => notifyListeners();
}

final routerProvider = Provider<GoRouter>((ref) {
  final refresh = _RouterRefresh();
  ref.listen(authProvider, (_, __) => refresh.poke());
  ref.listen(splashDoneProvider, (_, __) => refresh.poke());
  ref.listen(onboardedProvider, (_, __) => refresh.poke());
  ref.onDispose(refresh.dispose);

  final root = GlobalKey<NavigatorState>(debugLabel: 'root');

  return GoRouter(
    navigatorKey: root,
    initialLocation: '/splash',
    refreshListenable: refresh,
    redirect: (context, state) => redirectFor(
      status: ref.read(authProvider).status,
      splashDone: ref.read(splashDoneProvider),
      onboarded: ref.read(onboardedProvider),
      location: state.uri.toString(),
    ),
    routes: [
      GoRoute(path: '/splash', builder: (_, __) => const SplashScreen()),
      GoRoute(path: '/onboarding', builder: (_, __) => const OnboardingScreen()),
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(path: '/signup', builder: (_, __) => const SignupScreen()),
      GoRoute(path: '/forgot', builder: (_, __) => const ForgotPasswordScreen()),
      GoRoute(
        path: '/otp',
        builder: (_, s) {
          final q = s.uri.queryParameters;
          return OtpScreen(email: q['email'] ?? '', purpose: q['purpose'] == 'reset' ? OtpPurpose.reset : OtpPurpose.verify);
        },
      ),
      StatefulShellRoute.indexedStack(
        builder: (_, __, shell) => MainShell(shell: shell),
        branches: [
          StatefulShellBranch(routes: [GoRoute(path: '/home', builder: (_, __) => const HomeScreen())]),
          StatefulShellBranch(routes: [GoRoute(path: '/payments', builder: (_, __) => const PaymentsHubScreen())]),
          StatefulShellBranch(routes: [GoRoute(path: '/rewards', builder: (_, __) => const RewardsScreen())]),
          StatefulShellBranch(routes: [GoRoute(path: '/insights', builder: (_, __) => const InsightsScreen())]),
          StatefulShellBranch(routes: [GoRoute(path: '/profile', builder: (_, __) => const ProfileScreen())]),
        ],
      ),
      GoRoute(path: '/wallet', parentNavigatorKey: root, builder: (_, __) => const WalletScreen()),
      GoRoute(path: '/history', parentNavigatorKey: root, builder: (_, __) => const HistoryScreen()),
      GoRoute(path: '/radar', parentNavigatorKey: root, builder: (_, __) => const RadarScreen()),
      GoRoute(path: '/pay/send', parentNavigatorKey: root, builder: (_, __) => const SendRequestScreen(mode: PayMode.send)),
      GoRoute(path: '/pay/request', parentNavigatorKey: root, builder: (_, __) => const SendRequestScreen(mode: PayMode.request)),
      GoRoute(path: '/pay/receive', parentNavigatorKey: root, builder: (_, __) => const ReceiveScreen()),
      GoRoute(path: '/pay/topup', parentNavigatorKey: root, builder: (_, __) => const TopUpScreen()),
    ],
    errorBuilder: (_, s) => Scaffold(body: Center(child: Text('Page not found: ${s.uri}'))),
  );
});
