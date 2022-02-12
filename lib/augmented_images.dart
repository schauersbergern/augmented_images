
import 'dart:async';

import 'package:augmented_images/config.dart';
import 'package:flutter/services.dart';

class NativeMessenger {

  final EventChannel _messageChannel = const EventChannel(Config.eventChannelName);

  Stream<NativeMessage> get onMessage =>
      _messageChannel.receiveBroadcastStream().map(_toNativeMessage);

  NativeMessage _toNativeMessage(dynamic map) {
    if (map is Map) {
      var body = Map<String, dynamic>.from(map['body']);
      return NativeMessage(map['channel'], map['event'], body);
    }
    return NativeMessage(Config.emptyChannel, Config.emptyEvent, <String, dynamic>{});
  }

  static const MethodChannel _channel = MethodChannel('augmented_images');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}


class NativeMessage {
  final String channelName;
  final String eventName;
  final Map<String, dynamic> body;

  NativeMessage(this.channelName, this.eventName, this.body);
}