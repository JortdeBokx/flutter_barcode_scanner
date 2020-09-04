package com.github.jortdebokx.flutter_barcode_scanner

interface BarcodeReaderCallback {
    fun barcodeRead(data: String, type: String)
}