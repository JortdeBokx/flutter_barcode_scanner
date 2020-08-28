#import "FlutterBarcodeScannerPlugin.h"
#if __has_include(<flutter_barcode_scanner/flutter_barcode_scanner-Swift.h>)
#import <flutter_barcode_scanner/flutter_barcode_scanner-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_barcode_scanner-Swift.h"
#endif

@implementation FlutterBarcodeScannerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterBarcodeScannerPlugin registerWithRegistrar:registrar];
}
@end
