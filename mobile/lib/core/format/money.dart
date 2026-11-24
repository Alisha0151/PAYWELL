/// Money is held as integer minor units (e.g. paise/cents). Never use double for amounts.
class Money {
  const Money(this.minor, [this.currency = 'INR']);
  final int minor;
  final String currency;

  static const zero = Money(0);

  /// Parses an API value: decimal string ("1234.50"), int or double. Returns zero for null/garbage.
  factory Money.parse(Object? v, {String currency = 'INR'}) =>
      Money(_toMinor(v) ?? 0, currency);

  /// Parses user input ("1,250.5"). Returns null when empty, malformed, >2 decimals or not positive.
  static Money? tryParseInput(String raw, {String currency = 'INR'}) {
    final s = raw.trim().replaceAll(',', '');
    if (!RegExp(r'^\d+(\.\d{1,2})?$').hasMatch(s)) return null;
    final m = _toMinor(s);
    if (m == null || m <= 0) return null;
    return Money(m, currency);
  }

  static int? _toMinor(Object? v) {
    if (v == null) return null;
    String s = v is double ? v.toStringAsFixed(2) : v.toString().trim();
    if (s.isEmpty) return null;
    var neg = false;
    if (s.startsWith('-')) {
      neg = true;
      s = s.substring(1);
    }
    final parts = s.split('.');
    if (parts.length > 2) return null;
    final whole = int.tryParse(parts[0].isEmpty ? '0' : parts[0]);
    if (whole == null) return null;
    var frac = parts.length == 2 ? parts[1] : '';
    if (frac.length > 2) frac = frac.substring(0, 2);
    frac = frac.padRight(2, '0');
    final f = int.tryParse(frac);
    if (f == null) return null;
    final total = whole * 100 + f;
    return neg ? -total : total;
  }

  bool get isNegative => minor < 0;

  /// Plain decimal for API requests: "1250.50".
  String toApiString() {
    final a = minor.abs();
    return '${minor < 0 ? '-' : ''}${a ~/ 100}.${(a % 100).toString().padLeft(2, '0')}';
  }

  static String symbolFor(String c) =>
      const {'INR': '₹', 'USD': r'$', 'EUR': '€', 'GBP': '£'}[c] ?? '$c ';

  static String _group(int whole, bool indian) {
    final s = whole.toString();
    if (s.length <= 3) return s;
    if (!indian) {
      final b = StringBuffer();
      for (var i = 0; i < s.length; i++) {
        if (i > 0 && (s.length - i) % 3 == 0) b.write(',');
        b.write(s[i]);
      }
      return b.toString();
    }
    final last3 = s.substring(s.length - 3);
    var rest = s.substring(0, s.length - 3);
    final parts = <String>[];
    while (rest.length > 2) {
      parts.insert(0, rest.substring(rest.length - 2));
      rest = rest.substring(0, rest.length - 2);
    }
    if (rest.isNotEmpty) parts.insert(0, rest);
    return '${parts.join(',')},$last3';
  }

  /// "₹12,34,567.50" (Indian grouping for INR, international otherwise). [signed] prefixes + for credits.
  String format({bool signed = false}) {
    final a = minor.abs();
    final body = '${symbolFor(currency)}${_group(a ~/ 100, currency == 'INR')}.${(a % 100).toString().padLeft(2, '0')}';
    if (minor < 0) return '-$body';
    return signed && minor > 0 ? '+$body' : body;
  }

  @override
  bool operator ==(Object other) => other is Money && other.minor == minor && other.currency == currency;
  @override
  int get hashCode => Object.hash(minor, currency);
  @override
  String toString() => format();
}
