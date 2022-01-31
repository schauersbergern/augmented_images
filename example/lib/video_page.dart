import 'package:flutter/material.dart';

class VideoPage extends StatelessWidget {
  const VideoPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text("Ein cooles Video"),
          centerTitle: false,
          backgroundColor: Colors.red,
        ),
        body: const Center(
          child: Text("So much Video, so much Wow!"),
        ),
      );
  }
}