import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:augmented_images/augmented_images.dart';

void main() {
  const MethodChannel channel = MethodChannel('augmented_images');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await AugmentedImages.platformVersion, '42');
  });
}
