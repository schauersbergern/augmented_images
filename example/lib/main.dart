import 'package:augmented_images/augmented_images.dart';
import 'package:augmented_images_example/video_page.dart';
import 'package:flutter/material.dart';
import 'package:augmented_images/camera_view.dart';
import 'package:flutter/scheduler.dart';

void main() {
  runApp(const MaterialApp(home: MyApp(),));
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  //Todo: Use this only in Camera view, just provide callback when image is recognised
  NativeMessenger messenger = NativeMessenger();

  @override
  void initState() {
    super.initState();

    SchedulerBinding.instance?.addPostFrameCallback((_) {
      print("SchedulerBinding");
      messenger.onMessage.listen((pusher) {
        if (pusher.eventName == 'image_detected') {
          Navigator.of(context).push(MaterialPageRoute<VideoPage>(
            builder: (BuildContext context) {
              return const VideoPage();
            },
          ));
        }
      });
    });
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: const Center(
          child: AugmentedCameraView( triggerImagePaths: ['images/alma.jpg', 'images/default.jpg', 'images/nasa.jpg'] ),
        ),
      );
  }
}
