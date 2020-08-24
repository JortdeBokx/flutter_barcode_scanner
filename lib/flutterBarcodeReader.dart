import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_barcode_scanner/barcodeObjects.dart';

class flutterBarcodeReader {
  static const MethodChannel _channel =
      const MethodChannel('com.github.jortdebokx/flutter_barcode_scanner');
  static BarcodeChannelReader channelReader = BarcodeChannelReader(_channel);

  //Set target size before starting
  static Future<PreviewDetails> start({
    @required int height,
    @required int width,
    @required BarcodeHandler barcodeHandler,
    List<BarcodeFormat> formats,
  }) async {
    final _formats = formats ?? [BarcodeFormat.UNKNOWN];

    List<String> formatStrings = _formats
        .map((format) => format
            .toString()
            .split('.')[1]
            .replaceAll('UNKNOWN', 'ALL_FORMATS'))
        .toList(growable: false);

//TODO: finish this from this point onwards
    channelReader.setQrCodeHandler(barcodeHandler);
    var details = await _channel.invokeMethod('start', {
      'targetHeight': height,
      'targetWidth': width,
      'heartbeatTimeout': 0,
      'formats': formatStrings
    });

    int textureId = details["textureId"];
    num orientation = details["surfaceOrientation"];
    num surfaceHeight = details["surfaceHeight"];
    num surfaceWidth = details["surfaceWidth"];

    return new PreviewDetails(
        surfaceHeight, surfaceWidth, orientation, textureId);
  }

  static Future stop() {
    channelReader.setQrCodeHandler(null);
    return _channel.invokeMethod('stop').catchError(print);
  }

  static Future heartbeat() {
    return _channel.invokeMethod('heartbeat').catchError(print);
  }

  static Future<List<List<int>>> getSupportedSizes() {
    return _channel.invokeMethod('getSupportedSizes').catchError(print);
  }
}

typedef void BarcodeHandler(BarcodeResponse response);

class BarcodeChannelReader {
  BarcodeChannelReader(this.channel) {
    channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'read':
          if (qrCodeHandler != null) {
            assert(call.arguments is List<String>);
            String code = call.arguments[0];
            String formatString = call.arguments[1];
            BarcodeFormat format = BarcodeFormat.values.firstWhere(
                (e) => e.toString() == 'BarcodeFormat.' + formatString);
            if (format == null) {
              format = BarcodeFormat.UNKNOWN;
            }
            BarcodeResponse response = BarcodeResponse(code, format);
            qrCodeHandler(response);
          }
          break;
        default:
          print("QrChannelHandler: unknown method call received at "
              "${call.method}");
      }
    });
  }

  void setQrCodeHandler(BarcodeHandler qrch) {
    this.qrCodeHandler = qrch;
  }

  MethodChannel channel;
  BarcodeHandler qrCodeHandler;
}
