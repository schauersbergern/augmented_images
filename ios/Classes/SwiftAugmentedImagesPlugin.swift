import Flutter
import UIKit

public class SwiftAugmentedImagesPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "augmented_images", binaryMessenger: registrar.messenger())
    let instance = SwiftAugmentedImagesPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)

    let factory = FLNativeViewFactory(messenger: registrar.messenger())
    registrar.register(factory, withId: "com.schauersberger.augmentedimgs/cameraview")
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
