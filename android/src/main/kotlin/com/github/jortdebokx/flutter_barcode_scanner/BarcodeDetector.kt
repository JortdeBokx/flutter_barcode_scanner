package com.github.jortdebokx.flutter_barcode_scanner

import android.content.Context
import android.graphics.ImageFormat
import android.media.Image
import android.os.AsyncTask
import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import com.google.android.gms.vision.barcode.BarcodeDetector as GBarcodeDetector


class BarcodeDetector(private var communicator: BarcodeReaderCallback, context: Context, formats: Int) {
    private var detector: Detector<Barcode> = GBarcodeDetector.Builder(context.applicationContext).setBarcodeFormats(formats).build()
    private val imageToCheckLock: Lock = ReentrantLock()
    private val nextImageLock: Lock = ReentrantLock()
    private val isScheduled: AtomicBoolean = AtomicBoolean(false)
    private val needsScheduling: AtomicBoolean = AtomicBoolean(false)


    private val nextImageSet: AtomicBoolean = AtomicBoolean(false)
    private val imageToCheck: BarcodeImage = BarcodeImage()
    private val nextImage: BarcodeImage = BarcodeImage()

    private fun maybeStartProcessing() {
        // start processing, only if scheduling is needed and
        // there isn't currently a scheduled task.
        if (needsScheduling.get() && !isScheduled.get()) {
            isScheduled.set(true)
            BarcodeTask(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR)
        }
    }

    fun detect(image: Image) {
        needsScheduling.set(true)
        if (imageToCheckLock.tryLock()) {
            // copy image if not in use
            try {
                nextImageSet.set(false)
                imageToCheck.copyImage(image)
            } finally {
                imageToCheckLock.unlock()
            }
        } else if (nextImageLock.tryLock()) {
            // if first image buffer is in use, use second buffer
            // one or the other should always be free but if not this
            // frame is dropped..
            try {
                nextImageSet.set(true)
                nextImage.copyImage(image)
            } finally {
                nextImageLock.unlock()
            }
        }
        maybeStartProcessing()
    }

    internal class BarcodeImage {
        var width = 0
        var height = 0
        var yPlanePixelStride = 0
        var uPlanePixelStride = 0
        var vPlanePixelStride = 0
        var yPlaneRowStride = 0
        var uPlaneRowStride = 0
        var vPlaneRowStride = 0
        var yPlaneBytes: ByteArray = ByteArray(0)
        var uPlaneBytes: ByteArray = ByteArray(0)
        var vPlaneBytes: ByteArray = ByteArray(0)
        fun copyImage(image: Image) {
            val planes: Array<Image.Plane> = image.planes
            val yPlane: Image.Plane = planes[0]
            val uPlane: Image.Plane = planes[1]
            val vPlane: Image.Plane = planes[2]
            val yBufferDirect: ByteBuffer = yPlane.buffer
            val uBufferDirect: ByteBuffer = uPlane.buffer
            val vBufferDirect: ByteBuffer = vPlane.buffer
            if (yPlaneBytes.size != yBufferDirect.capacity()) {
                yPlaneBytes = ByteArray(yBufferDirect.capacity())
            }
            if (uPlaneBytes.size != uBufferDirect.capacity()) {
                uPlaneBytes = ByteArray(uBufferDirect.capacity())
            }
            if (vPlaneBytes.size != vBufferDirect.capacity()) {
                vPlaneBytes = ByteArray(vBufferDirect.capacity())
            }
            yBufferDirect.get(yPlaneBytes)
            uBufferDirect.get(uPlaneBytes)
            vBufferDirect.get(vPlaneBytes)
            width = image.width
            height = image.height
            yPlanePixelStride = yPlane.pixelStride
            uPlanePixelStride = uPlane.pixelStride
            vPlanePixelStride = vPlane.pixelStride
            yPlaneRowStride = yPlane.rowStride
            uPlaneRowStride = uPlane.rowStride
            vPlaneRowStride = vPlane.rowStride
        }

        fun toNv21(greyScale: Boolean): ByteBuffer? {
            val halfWidth = width / 2
            val numPixels = width * height
            val nv21ImageBytes = ByteArray(numPixels * 2)
            if (greyScale) {
                Arrays.fill(nv21ImageBytes, 127.toByte())
            }
            val nv21Buffer: ByteBuffer = ByteBuffer.wrap(nv21ImageBytes)
            for (i in 0 until height) {
                nv21Buffer.put(yPlaneBytes, i * yPlaneRowStride, width)
            }
            if (!greyScale) {
                for (row in 0 until height / 2) {
                    val uRow = row * uPlaneRowStride
                    val vRow = row * vPlaneRowStride
                    var count = 0
                    var u = 0
                    var v = 0
                    while (count < halfWidth) {
                        nv21Buffer.put(uPlaneBytes!![uRow + u])
                        nv21Buffer.put(vPlaneBytes!![vRow + v])
                        u += uPlanePixelStride
                        v += vPlanePixelStride
                        count++
                    }
                }
            }
            return nv21Buffer
        }
    }

    private class BarcodeTask(barcodeDetector: BarcodeDetector) : AsyncTask<Void, Void, SparseArray<Barcode>>() {
        private val barDetector: WeakReference<BarcodeDetector> = WeakReference(barcodeDetector)
        override fun doInBackground(vararg voids: Void): SparseArray<Barcode>? {
            val barDetector: BarcodeDetector = barDetector.get() ?: return null
            barDetector.needsScheduling.set(false)
            barDetector.isScheduled.set(false)
            val imageBuffer: ByteBuffer
            val width: Int
            val height: Int
            if (barDetector.nextImageSet.get()) {
                try {
                    barDetector.nextImageLock.lock()
                    imageBuffer = barDetector.nextImage.toNv21(false)!!
                    width = barDetector.nextImage.width
                    height = barDetector.nextImage.height
                } finally {
                    barDetector.nextImageLock.unlock()
                }
            } else {
                try {
                    barDetector.imageToCheckLock.lock()
                    imageBuffer = barDetector.imageToCheck.toNv21(false)!!
                    width = barDetector.imageToCheck.width
                    height = barDetector.imageToCheck.height
                } finally {
                    barDetector.imageToCheckLock.unlock()
                }
            }
            val builder: Frame.Builder = Frame.Builder().setImageData(imageBuffer, width, height, ImageFormat.NV21)
            return barDetector.detector.detect(builder.build())
        }

        override fun onPostExecute(detectedItems: SparseArray<Barcode>?) {
            val qrDetector: BarcodeDetector = barDetector.get() ?: return
            if (detectedItems != null) {
                for (i in 0 until detectedItems.size()) {
                    val format = BarcodeFormats.stringFromInt(detectedItems.valueAt(i)!!.format)
                    qrDetector.communicator.barcodeRead(detectedItems.valueAt(i)!!.rawValue, format)
                }
            }

            // if needed keep processing.
            qrDetector.maybeStartProcessing()
        }

    }
}