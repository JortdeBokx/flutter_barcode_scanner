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
