import 'package:dio/dio.dart';

import '../format/ids.dart';
import 'api_exception.dart';
import 'token_storage.dart';

const String kApiBaseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8080');

class ApiClient {
  ApiClient({required this.storage, required this.onUnauthorized, String? baseUrl, Dio? dio})
      : dio = dio ?? Dio(BaseOptions(
            baseUrl: baseUrl ?? kApiBaseUrl,
            connectTimeout: const Duration(seconds: 10),
            receiveTimeout: const Duration(seconds: 20),
            headers: {'Accept': 'application/json'},
          )) {
    this.dio.interceptors.add(InterceptorsWrapper(onRequest: _onRequest, onError: _onError));
  }

  final Dio dio;
  final TokenStorage storage;
  final void Function() onUnauthorized;

  static const _authFree = ['/api/v1/auth/login', '/api/v1/auth/signup', '/api/v1/auth/refresh', '/api/v1/auth/forgot-password', '/api/v1/auth/reset-password'];
  bool _isAuthFree(String path) => _authFree.any(path.endsWith);

  Future<void> _onRequest(RequestOptions o, RequestInterceptorHandler h) async {
    if (!_isAuthFree(o.path)) {
      final t = await storage.readAccess();
      if (t != null) o.headers['Authorization'] = 'Bearer $t';
    }
    h.next(o);
  }

  Future<void> _onError(DioException e, ErrorInterceptorHandler h) async {
    final req = e.requestOptions;
    if (e.response?.statusCode == 401 && !_isAuthFree(req.path) && req.extra['retried'] != true) {
      final ok = await _refresh();
      if (ok) {
        try {
          req.extra['retried'] = true;
          final t = await storage.readAccess();
          req.headers['Authorization'] = 'Bearer $t';
          return h.resolve(await dio.fetch<dynamic>(req));
        } catch (err) {
          if (err is DioException) return h.next(err);
        }
      }
      onUnauthorized();
    }
    h.next(e);
  }

  Future<bool> _refresh() async {
    final rt = await storage.readRefresh();
    if (rt == null) return false;
    try {
      // Separate Dio so the interceptor does not recurse.
      final r = await Dio(BaseOptions(baseUrl: dio.options.baseUrl, connectTimeout: const Duration(seconds: 10)))
          .post<dynamic>('/api/v1/auth/refresh', data: {'refreshToken': rt});
      final d = r.data;
      if (d is Map && d['accessToken'] is String) {
        await storage.saveTokens(access: d['accessToken'] as String, refresh: d['refreshToken'] as String?);
        return true;
      }
    } catch (_) {}
    return false;
  }

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) async {
    try {
      return (await dio.get<dynamic>(path, queryParameters: query)).data;
    } catch (e) {
      throw ApiException.from(e);
    }
  }

  Future<dynamic> post(String path, {Object? body, String? idempotencyKey}) async {
    try {
      final r = await dio.post<dynamic>(path, data: body,
          options: idempotencyKey == null ? null : Options(headers: {'Idempotency-Key': idempotencyKey}));
      return r.data;
    } catch (e) {
      throw ApiException.from(e);
    }
  }

  Future<dynamic> patch(String path, {Object? body}) async {
    try {
      return (await dio.patch<dynamic>(path, data: body)).data;
    } catch (e) {
      throw ApiException.from(e);
    }
  }

  /// Payment/money-moving POST: always carries an Idempotency-Key. Pass the SAME key when retrying the same user action.
  Future<dynamic> postIdempotent(String path, {required Object body, String? key}) =>
      post(path, body: body, idempotencyKey: key ?? newIdempotencyKey());
}
