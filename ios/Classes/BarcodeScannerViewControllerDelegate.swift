
import Foundation

protocol BarcodeScannerViewControllerDelegate {
    func didScanBarcodeWithResult(_ controller: BarcodeScannerViewController?,
                                  scanResult: ScanResult
    )
    
    func didFailWithErrorCode(_ controller: BarcodeScannerViewController?,
                              errorCode: String
    )
}