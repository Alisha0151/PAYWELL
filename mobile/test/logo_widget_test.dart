import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:paywell/core/brand/paywell_logo.dart';

void main() {
  testWidgets('PaywellLogo renders on light and dark styles', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: Column(children: [
      PaywellLogo(width: 200),
      PaywellLogo(width: 80, style: PaywellLogoStyle.night, showWordmark: false),
    ])));
    expect(find.byType(PaywellLogo), findsNWidgets(2));
  });

  testWidgets('launch logo reports finished (reduced motion path)', (tester) async {
    var done = false;
    await tester.pumpWidget(MediaQuery(
      data: const MediaQueryData(disableAnimations: true),
      child: MaterialApp(home: PaywellLaunchLogo(onFinished: () => done = true)),
    ));
    await tester.pump(const Duration(seconds: 3));
    expect(done, isTrue);
  });
}
