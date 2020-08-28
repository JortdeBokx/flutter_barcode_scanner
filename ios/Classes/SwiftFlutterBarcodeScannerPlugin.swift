import Flutter
import UIKit

public class SwiftFlutterBarcodeScannerPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "com.github.jortdebokx.flutter_barcode_scanner", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterBarcodeScannerPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
