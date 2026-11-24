import 'package:flutter_secure_storage/flutter_secure_storage.dart';

abstract class TokenStorage {
  Future<String?> readAccess();
  Future<String?> readRefresh();
  Future<void> saveTokens({required String access, String? refresh});
  Future<void> clear();
  Future<bool> readOnboarded();
  Future<void> setOnboarded();
}

/// Tokens live only in the platform keystore/keychain via flutter_secure_storage.
class SecureTokenStorage implements TokenStorage {
  SecureTokenStorage([FlutterSecureStorage? s]) : _s = s ?? const FlutterSecureStorage();
  final FlutterSecureStorage _s;
  static const _a = 'pw_access', _r = 'pw_refresh', _o = 'pw_onboarded';

  @override
  Future<String?> readAccess() => _s.read(key: _a);
  @override
  Future<String?> readRefresh() => _s.read(key: _r);
  @override
  Future<void> saveTokens({required String access, String? refresh}) async {
    await _s.write(key: _a, value: access);
    if (refresh != null) await _s.write(key: _r, value: refresh);
  }

  @override
  Future<void> clear() async {
    await _s.delete(key: _a);
    await _s.delete(key: _r);
  }

  @override
  Future<bool> readOnboarded() async => (await _s.read(key: _o)) == '1';
  @override
  Future<void> setOnboarded() => _s.write(key: _o, value: '1');
}

/// In-memory implementation for tests.
class MemoryTokenStorage implements TokenStorage {
  String? access, refresh;
  bool onboarded = false;
  @override
  Future<String?> readAccess() async => access;
  @override
  Future<String?> readRefresh() async => refresh;
  @override
  Future<void> saveTokens({required String access, String? refresh}) async {
    this.access = access;
    if (refresh != null) this.refresh = refresh;
  }

  @override
  Future<void> clear() async {
    access = null;
    refresh = null;
  }

  @override
  Future<bool> readOnboarded() async => onboarded;
  @override
  Future<void> setOnboarded() async => onboarded = true;
}
