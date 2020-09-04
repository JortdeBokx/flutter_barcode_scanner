package com.github.jortdebokx.flutter_barcode_scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Build
import com.google.android.gms.vision.CameraSource
import java.io.IOException
import java.lang.Exception


internal class BarcodeReader(width: Int, height: Int, private val context: Context, barcodeFormats: Int,
                             private val startedCallback: BarcodeReaderStartedCallback, communicator: BarcodeReaderCallback,
                             texture: SurfaceTexture) {
    lateinit var barCamera: BarcodeCamera
    private var heartbeat: Heartbeat? = null
    private var camera: CameraSource? = null

    @Throws(IOException::class, NoPermissionException::class, Exception::class)
    fun start(heartBeatTimeout: Int) {
        if (!hasCameraHardware(context)) {
            throw Exception(Exception.Reason.noHardware)
        }
        if (!checkCameraPermission(context)) {
            throw NoPermissionException()
        } else {
            continueStarting(heartBeatTimeout)
        }
    }

    @Throws(IOException::class)
    private fun continueStarting(heartBeatTimeout: Int) {
        try {
            if (heartBeatTimeout > 0) {
                if(heartbeat != null) {
                    heartbeat?.stop()
                }
                heartbeat = Heartbeat(heartBeatTimeout, Runnable { stop() })
            }
            barCamera.start()
            startedCallback.started()
        } catch (t: Throwable) {
            startedCallback.startingFailed(t)
        }
    }

    fun stop() {
        if (heartbeat != null) {
            heartbeat?.stop()
        }
        if (camera != null) {
            camera?.stop()
            // also stops detector
            camera?.release()
            camera = null
        }
        barCamera.stop()
    }

    fun heartBeat() {
        if (heartbeat != null) {
            heartbeat?.beat()
        }
    }

    private fun hasCameraHardware(context: Context): Boolean {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    private fun checkCameraPermission(context: Context): Boolean {
        val permissions = arrayOf<String>(Manifest.permission.CAMERA)
        val res: Int = context.checkCallingOrSelfPermission(permissions[0])
        return res == PackageManager.PERMISSION_GRANTED
    }

    interface BarcodeReaderStartedCallback {
        fun started()
        fun startingFailed(t: Throwable)
    }

    class Exception internal constructor(private val reason: Reason) : java.lang.Exception("QR reader failed because $reason") {
        fun reason(): Reason {
            return reason
        }

        internal enum class Reason {
            noHardware, noPermissions, noBackCamera
        }

    }

    init {
        if (Build.VERSION.SDK_INT >= 21) {
            barCamera = BarcodeCamera(width, height, context, texture, BarcodeDetector(communicator, context, barcodeFormats))
        }
    }
}
