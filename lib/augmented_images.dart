
import 'dart:async';

import 'package:augmented_images/config.dart';
import 'package:flutter/services.dart';

class AugmentedImages {

  final EventChannel _messageChannel = const EventChannel(Config.eventChannelName);

  Stream<PusherMessage> get onMessage =>
      _messageChannel.receiveBroadcastStream().map(_toPusherMessage);

  PusherMessage _toPusherMessage(dynamic map) {
    if (map is Map) {
      var body = Map<String, dynamic>.from(map['body']);
      return PusherMessage(map['channel'], map['event'], body);
    }
    return PusherMessage(Config.emptyChannel, Config.emptyEvent, <String, dynamic>{});
  }

  static const MethodChannel _channel = MethodChannel('augmented_images');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}


class PusherMessage {
  final String channelName;
  final String eventName;
  final Map<String, dynamic> body;

  PusherMessage(this.channelName, this.eventName, this.body);
}