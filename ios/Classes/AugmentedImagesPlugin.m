#import "AugmentedImagesPlugin.h"
#if __has_include(<augmented_images/augmented_images-Swift.h>)
#import <augmented_images/augmented_images-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "augmented_images-Swift.h"
#endif

@implementation AugmentedImagesPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftAugmentedImagesPlugin registerWithRegistrar:registrar];
}
@end
