import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_barcode_scanner/barcodeObjects.dart';

final WidgetBuilder _defaultNotStartedBuilder =
    (context) => new Text("Loading Scanner Camera...");
final WidgetBuilder _defaultOffscreenBuilder =
    (context) => new Text("Scanner Camera Paused.");
final Function _defaultOnError = (BuildContext context, Object error) {
  print("Error reading from scanner camera: $error");
  return new Text("Error reading from scanner camera...");
};

class BarcodeScanner extends StatefulWidget {
  final BoxFit fit;
  final Function(String, String) qrCodeCallback;
  final Widget child;
  final WidgetBuilder notStartedBuilder;
  final WidgetBuilder offscreenBuilder;
  final Function onError;
  final List<BarcodeFormat> formats;

  BarcodeScanner({
    Key key,
    @required this.qrCodeCallback,
    this.child,
    this.fit = BoxFit.cover,
    WidgetBuilder notStartedBuilder,
    WidgetBuilder offscreenBuilder,
    Function onError,
    this.formats,
  })  : notStartedBuilder = notStartedBuilder ?? _defaultNotStartedBuilder,
        offscreenBuilder =
            offscreenBuilder ?? notStartedBuilder ?? _defaultOffscreenBuilder,
        onError = onError ?? _defaultOnError,
        assert(fit != null),
        super(key: key);

  @override
  BarcodeScannerState createState() => BarcodeScannerState();
}

class BarcodeScannerState extends State<BarcodeScanner>
    with WidgetsBindingObserver {
  bool onScreen = true;
  Future<PreviewDetails> _asyncInitOnce;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  dispose() {
    WidgetsBinding.instance.removeObserver(this);
    stop();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      setState(() => onScreen = true);
    } else {
      if (_asyncInitOnce != null && onScreen) {
        FlutterQrReader.stop();
      }
      setState(() {
        onScreen = false;
        _asyncInitOnce = null;
      });
    }
  }
}
