import 'package:dio/dio.dart';

class ApiException implements Exception {
  ApiException(this.message, {this.statusCode, this.code, this.isNetwork = false});
  final String message;
  final int? statusCode;
  final String? code;
  final bool isNetwork;

  bool get isUnauthorized => statusCode == 401;

  factory ApiException.from(Object e) {
    if (e is ApiException) return e;
    if (e is DioException) {
      final data = e.response?.data;
      String? msg;
      String? code;
      if (data is Map) {
        msg = (data['message'] ?? data['detail'] ?? data['error'])?.toString();
        code = data['code']?.toString();
      }
      final sc = e.response?.statusCode;
      if (sc == null) {
        return ApiException('Cannot reach the server. Check your connection and try again.', isNetwork: true);
      }
      return ApiException(msg ?? _default(sc), statusCode: sc, code: code);
    }
    return ApiException('Something went wrong. Please try again.');
  }

  static String _default(int sc) {
    if (sc == 401) return 'Your session has expired. Please sign in again.';
    if (sc == 403) return 'You are not allowed to do that.';
    if (sc == 404) return 'Not found.';
    if (sc == 409) return 'This request conflicts with an earlier one.';
    if (sc == 429) return 'Too many attempts. Please wait a moment.';
    if (sc >= 500) return 'The server had a problem. Please try again later.';
    return 'Request failed ($sc).';
  }

  @override
  String toString() => message;
}
