import 'package:flutter_test/flutter_test.dart';
import 'package:paywell/core/router/app_router.dart';
import 'package:paywell/data/providers.dart';

String? r(AuthStatus s, String loc, {bool splash = true, bool onboarded = true}) =>
    redirectFor(status: s, splashDone: splash, onboarded: onboarded, location: loc);

void main() {
  test('stays on splash until it finishes', () {
    expect(r(AuthStatus.unauthenticated, '/login', splash: false), '/splash');
    expect(r(AuthStatus.unknown, '/splash'), isNull);
  });
  test('unauthenticated users are sent to onboarding or login', () {
    expect(r(AuthStatus.unauthenticated, '/splash', onboarded: false), '/onboarding');
    expect(r(AuthStatus.unauthenticated, '/splash'), '/login');
    expect(r(AuthStatus.unauthenticated, '/wallet'), '/login');
    expect(r(AuthStatus.unauthenticated, '/signup'), isNull);
  });
  test('authenticated users skip public pages', () {
    expect(r(AuthStatus.authenticated, '/login'), '/home');
    expect(r(AuthStatus.authenticated, '/wallet'), isNull);
  });
}
