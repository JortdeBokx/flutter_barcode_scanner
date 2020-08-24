package com.github.jortdebokx.flutter_barcode_scanner


import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraMetadata.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import java.lang.Exception


class BarcodeCamera(width: Int, height: Int, private var context: Context, private var texture: SurfaceTexture, private var detector: BarcodeDetector) {
    private val ORIENTATIONS: SparseIntArray = SparseIntArray()

    init{
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    private var targetWidth = width
    private var targetHeight = height
    private lateinit var size: Size
    private lateinit var reader: ImageReader
    private lateinit var previewBuilder: CaptureRequest.Builder
    private lateinit var previewSession: CameraCaptureSession
    private lateinit var jpegSizes: Array<Size>
    private var orientation = 0
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraCharacteristics: CameraCharacteristics

    fun getWidth(): Int {
        return size.width
    }

    fun getHeight(): Int {
        return size.height
    }

    fun getOrientation(): Int {
        return orientation
    }

    @Throws(BarcodeReader.Exception::class)
    fun start() {
        val manager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                ?: throw RuntimeException("Unable to get camera manager.")
        var cameraId: String? = null
        try {
            val cameraIdList: Array<String> = manager.cameraIdList
            for (id in cameraIdList) {
                val cameraCharacteristics: CameraCharacteristics = manager.getCameraCharacteristics(id)
                val integer: Int = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)!!
                if (integer == LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
        if (cameraId == null) {
            throw BarcodeReader.Exception(BarcodeReader.Exception.Reason.noBackCamera)
        }
        try {
            cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
            val map: StreamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            // it seems as though the orientation is already corrected, so setting to 0
            // orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            orientation = 0
            size = getAppropriateSize(map.getOutputSizes(SurfaceTexture::class.java))
            jpegSizes = map.getOutputSizes(ImageFormat.JPEG)
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    startCamera()
                }

                override fun onDisconnected(device: CameraDevice) {}
                override fun onError(device: CameraDevice, error: Int) {
                }
            }, null)
        } catch (e: CameraAccessException) {
        }
    }

    private fun afMode(cameraCharacteristics: CameraCharacteristics): Int? {
        val afModes: IntArray = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                ?: return null
        val modes: HashSet<Int> = HashSet(afModes.size * 2)
        for (afMode in afModes) {
            modes.add(afMode)
        }
        return if (modes.contains(CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
            CONTROL_AF_MODE_CONTINUOUS_PICTURE
        } else if (modes.contains(CONTROL_AF_MODE_CONTINUOUS_VIDEO)) {
            CONTROL_AF_MODE_CONTINUOUS_VIDEO
        } else if (modes.contains(CONTROL_AF_MODE_AUTO)) {
            CONTROL_AF_MODE_AUTO
        } else {
            null
        }
    }

    private fun startCamera() {
        val list: MutableList<Surface> = ArrayList()
        val jpegSize: Size = getAppropriateSize(jpegSizes)
        val width: Int = jpegSize.width
        val height: Int = jpegSize.height
        reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 5)
        list.add(reader.surface)


        val imageAvailableListener: ImageReader.OnImageAvailableListener = ImageReader.OnImageAvailableListener(){
            try {
                var image: Image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
                detector.detect(image)
                image.close()
            }catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        reader.setOnImageAvailableListener(imageAvailableListener, null)
        texture.setDefaultBufferSize(size.width, size.height)
        list.add(Surface(texture))
        try {
            previewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)!!
            previewBuilder.addTarget(list[0])
            previewBuilder.addTarget(list[1])
            val afMode = afMode(cameraCharacteristics)
            previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            if (afMode != null) {
                previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, afMode)
                if (afMode == CONTROL_AF_MODE_AUTO) {
                    previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
                } else {
                    previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        try {
            cameraDevice?.createCaptureSession(list, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    previewSession = session
                    startPreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                }
            }, null)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun startPreview() {
        val listener: CaptureCallback = object: CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
            }
        }
        if (cameraDevice == null) return
        try {
            previewSession.setRepeatingRequest(previewBuilder.build(), listener, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        if (cameraDevice != null) {
            cameraDevice?.close()
        }
        if (reader != null) {
            reader.close()
        }
    }

    private fun getAppropriateSize(sizes: Array<Size>?): Size {
        // assume sizes is never 0
        if (sizes!!.size == 1) {
            return sizes[0]
        }
        var s: Size = sizes[0]
        val s1: Size = sizes[1]
        if (s1.getWidth() > s.getWidth() || s1.getHeight() > s.getHeight()) {
            // ascending
            if (orientation % 180 == 0) {
                for (size in sizes) {
                    s = size
                    if (size.getHeight() > targetHeight && size.getWidth() > targetWidth) {
                        break
                    }
                }
            } else {
                for (size in sizes) {
                    s = size
                    if (size.getHeight() > targetWidth && size.getWidth() > targetHeight) {
                        break
                    }
                }
            }
        } else {
            // descending
            if (orientation % 180 == 0) {
                for (size in sizes) {
                    if (size.getHeight() < targetHeight || size.getWidth() < targetWidth) {
                        break
                    }
                    s = size
                }
            } else {
                for (size in sizes) {
                    if (size.getHeight() < targetWidth || size.getWidth() < targetHeight) {
                        break
                    }
                    s = size
                }
            }
        }
        return s
    }
}