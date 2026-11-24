import 'package:flutter_test/flutter_test.dart';
import 'package:paywell/core/format/money.dart';

void main() {
  test('parses decimal strings to minor units without floating point', () {
    expect(Money.parse('1234.50').minor, 123450);
    expect(Money.parse('0.07').minor, 7);
    expect(Money.parse('-5.5').minor, -550);
    expect(Money.parse(null).minor, 0);
  });

  test('user input must be positive with at most 2 decimals', () {
    expect(Money.tryParseInput('1,250.5')?.minor, 125050);
    expect(Money.tryParseInput('0'), isNull);
    expect(Money.tryParseInput('1.234'), isNull);
    expect(Money.tryParseInput('abc'), isNull);
  });

  test('api string and Indian grouping', () {
    expect(const Money(123456750).toApiString(), '1234567.50');
    expect(const Money(123456750).format(), '₹12,34,567.50');
    expect(const Money(-100).format(), '-₹1.00');
    expect(const Money(100, 'USD').format(signed: true), '+\$1.00');
  });
}
