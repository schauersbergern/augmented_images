import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'dart:io' show Platform;

class AugmentedCameraView extends StatefulWidget {
  const AugmentedCameraView({Key? key}) : super(key: key);

  @override
  State<StatefulWidget> createState() => CameraViewState();
}

class CameraViewState extends State<AugmentedCameraView> {

  // This is used in the platform side to register the view.
  static const String viewType = 'com.schauersberger.augmentedimgs/cameraview';
  // Pass parameters to the platform side.
  static const Map<String, dynamic> creationParams = <String, dynamic>{};

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        getCameraView(),
        const Image(image: AssetImage('assets/fit_to_scan.png', package: 'augmented_images'))
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
      return const UiKitView(
        viewType: viewType,
        layoutDirection: TextDirection.ltr,
        creationParams: creationParams,
        creationParamsCodec: StandardMessageCodec(),
      );
    } else {
      return const Text('This platform is not supported');
    }
  }

}