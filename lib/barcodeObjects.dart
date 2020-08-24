import 'dart:io';

import 'package:flutter/widgets.dart';
import 'package:native_device_orientation/native_device_orientation.dart';

enum BarcodeFormat {
  AZTEC,
  CODE_128,
  CODE_39,
  CODE_93,
  CODABAR,
  DATA_MATRIX,
  EAN_13,
  EAN_8,
  ITF,
  PDF417,
  QR_CODE,
  UPC_A,
  UPC_E,
  UNKNOWN,
}

class BarcodeResponse {
  final String code;
  final BarcodeFormat format;
  BarcodeResponse(this.code, this.format);
}

class PreviewDetails {
  num height;
  num width;
  num orientation;
  int textureId;

  PreviewDetails(this.height, this.width, this.orientation, this.textureId);
}

class Preview extends StatelessWidget {
  final double height;
  final double width;
  final double targetWidth, targetHeight;
  final int textureId;
  final int orientation;
  final BoxFit fit;

  Preview({
    @required PreviewDetails previewDetails,
    @required this.targetHeight,
    @required this.targetWidth,
    @required this.fit,
  })  : assert(previewDetails != null),
        textureId = previewDetails.textureId,
        height = previewDetails.height.toDouble(),
        width = previewDetails.width.toDouble(),
        orientation = previewDetails.orientation;

  @override
  Widget build(BuildContext context) {
    double frameHeight, frameWidth;

    return NativeDeviceOrientationReader(
      builder: (context) {
        var nativeOrientation =
            NativeDeviceOrientationReader.orientation(context);

        int baseOrientation = 0;
        if (orientation != 0 && (width > height)) {
          baseOrientation = orientation ~/ 90;
          frameHeight = height;
          frameWidth = width;
        } else {
          frameWidth = height;
          frameHeight = width;
        }

        int nativeOrientationInt;
        switch (nativeOrientation) {
          case NativeDeviceOrientation.landscapeLeft:
            nativeOrientationInt = Platform.isAndroid ? 3 : 1;
            break;
          case NativeDeviceOrientation.landscapeRight:
            nativeOrientationInt = Platform.isAndroid ? 1 : 3;
            break;
          case NativeDeviceOrientation.portraitDown:
            nativeOrientationInt = 2;
            break;
          case NativeDeviceOrientation.portraitUp:
          case NativeDeviceOrientation.unknown:
            nativeOrientationInt = 0;
        }

        return new FittedBox(
          fit: fit,
          child: new RotatedBox(
            quarterTurns: baseOrientation + nativeOrientationInt,
            child: new SizedBox(
              height: frameHeight,
              width: frameWidth,
              child: new Texture(textureId: textureId),
            ),
          ),
        );
      },
    );
  }
}
