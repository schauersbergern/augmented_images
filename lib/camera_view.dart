import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:augmented_images/config.dart';
import 'dart:io' show Platform;

class AugmentedCameraView extends StatefulWidget {
  const AugmentedCameraView({Key? key, required this.triggerImagePaths}) : super(key: key);

  final List<String> triggerImagePaths;

  @override
  State<StatefulWidget> createState() => CameraViewState();
}

class CameraViewState extends State<AugmentedCameraView> {

  // This is used in the platform side to register the view.
  static const String viewType = 'com.schauersberger.augmentedimgs/cameraview';
  // Pass parameters to the platform side.
  var creationParams = <String, dynamic>{};

  @override
  Widget build(BuildContext context) {

    creationParams[Config.triggerImagePaths] = widget.triggerImagePaths;

    return Stack(
      children: [
        getCameraView(),
          const Image(
            width: double.infinity,
            height: double.infinity,
            fit: BoxFit.fitWidth,
            image: AssetImage('assets/fit_to_scan.png', package: 'augmented_images'),
          ),
      ],
    );
  }

  Widget getCameraView() {
    if (Platform.isAndroid) {
      return PlatformViewLink(
        viewType: viewType,
        surfaceFactory:
            (BuildContext context, PlatformViewController controller) {
          return AndroidViewSurface(
            controller: controller as AndroidViewController,
            gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
            hitTestBehavior: PlatformViewHitTestBehavior.opaque,
          );
        },
        onCreatePlatformView: (PlatformViewCreationParams params) {
          return PlatformViewsService.initSurfaceAndroidView(
            id: params.id,
            viewType: viewType,
            layoutDirection: TextDirection.ltr,
            creationParams: creationParams,
            creationParamsCodec: const StandardMessageCodec(),
            onFocus: () {
              params.onFocusChanged(true);
            },
          )
            ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
            ..create();
        },
      );
    } else if (Platform.isIOS) {
      return UiKitView(
        viewType: viewType,
        layoutDirection: TextDirection.ltr,
        creationParams: creationParams,
        creationParamsCodec: const StandardMessageCodec(),
      );
    } else {
      return const Text('This platform is not supported');
    }
  }

}