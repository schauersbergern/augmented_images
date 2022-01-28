import 'package:flutter/material.dart';

class VideoPage extends StatelessWidget {
  const VideoPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text("Ready to pair!"),
          centerTitle: false,
          backgroundColor: Colors.red,
        ),
        body: const Center(
          child: Text("Hier steht was!"),
        ),
      );
  }
}