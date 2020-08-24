import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_barcode_scanner/barcodeObjects.dart';
import './barcodeObjects.dart';
import './flutterBarcodeReader.dart';

export './barcodeObjects.dart';

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
  final Function(BarcodeResponse) barcodeCallback;
  final Widget child;
  final WidgetBuilder notStartedBuilder;
  final WidgetBuilder offscreenBuilder;
  final Function onError;
  final List<BarcodeFormat> formats;

  BarcodeScanner({
    Key key,
    @required this.barcodeCallback,
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
        FlutterBarcodeReader.stop();
      }
      setState(() {
        onScreen = false;
        _asyncInitOnce = null;
      });
    }
  }

  Future<PreviewDetails> _asyncInit(num height, num width) async {
    var previewDetails = await FlutterBarcodeReader.start(
      height: height.toInt(),
      width: width.toInt(),
      barcodeHandler: widget.barcodeCallback,
      formats: widget.formats,
    );
    return previewDetails;
  }

  /// This method can be used to restart scanning
  ///  the event that it was paused.
  void restart() {
    (() async {
      await FlutterBarcodeReader.stop();
      setState(() {
        _asyncInitOnce = null;
      });
    })();
  }

  /// This method can be used to manually stop the
  /// camera.
  void stop() {
    (() async {
      await FlutterBarcodeReader.stop();
    })();
  }

  @override
  Widget build(BuildContext context) {
    return new LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
      if (_asyncInitOnce == null && onScreen) {
        _asyncInitOnce =
            _asyncInit(constraints.maxHeight, constraints.maxWidth);
      } else if (!onScreen) {
        return widget.offscreenBuilder(context);
      }

      return new FutureBuilder(
        future: _asyncInitOnce,
        builder: (BuildContext context, AsyncSnapshot<PreviewDetails> details) {
          switch (details.connectionState) {
            case ConnectionState.none:
            case ConnectionState.waiting:
              return widget.notStartedBuilder(context);
            case ConnectionState.done:
              if (details.hasError) {
                debugPrint(details.error.toString());
                return widget.onError(context, details.error);
              }
              Widget preview = new SizedBox(
                height: constraints.maxHeight,
                width: constraints.maxWidth,
                child: Preview(
                  previewDetails: details.data,
                  targetHeight: constraints.maxHeight,
                  targetWidth: constraints.maxWidth,
                  fit: widget.fit,
                ),
              );

              if (widget.child != null) {
                return new Stack(
                  children: [
                    preview,
                    widget.child,
                  ],
                );
              }
              return preview;

            default:
              throw new AssertionError(
                  "${details.connectionState} not supported.");
          }
        },
      );
    });
  }
}
