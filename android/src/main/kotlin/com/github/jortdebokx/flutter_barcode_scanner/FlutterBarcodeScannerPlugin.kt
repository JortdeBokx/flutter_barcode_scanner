package com.github.jortdebokx.flutter_barcode_scanner

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.view.TextureRegistry
import io.flutter.view.TextureRegistry.SurfaceTextureEntry
import java.io.IOException


/** FlutterBarcodeScannerPlugin */
class FlutterBarcodeScannerPlugin() : FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.RequestPermissionsResultListener, BarcodeReaderCallback, BarcodeReader.BarcodeReaderStartedCallback {
  private val REQUEST_PERMISSION = 50
  private var lastHeartbeatTimeout: Int? = null
  private var waitingForPermissionResult = false
  private var permissionDenied = false
  private lateinit var readingInstance: ReadingInstance
  private lateinit var context: Context
  private lateinit var activity: Activity
  private lateinit var textures: TextureRegistry
  private lateinit var channel : MethodChannel

//  fun registerWith(registrar: PluginRegistry.Registrar) {
//    val channel = MethodChannel(registrar.messenger(), "com.github.jortdebokx/flutter_barcode_scanner")
//    val flutterBarcodeScannerPlugin = FlutterBarcodeScannerPlugin(channel, registrar.activity(), registrar.textures())
//    channel.setMethodCallHandler(flutterBarcodeScannerPlugin)
//    registrar.addRequestPermissionsResultListener(flutterBarcodeScannerPlugin)
//  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.github.jortdebokx/flutter_barcode_scanner")
    context = flutterPluginBinding.applicationContext
    textures = flutterPluginBinding.textureRegistry
    channel.setMethodCallHandler(this)

  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    println(call.method)
    if(call.method=="start"){
        if (permissionDenied) {
          permissionDenied = false
          result.error("BarcodeReader_ERROR", "noPermission", null)
        } else {
          // stopReader();
          // result.error("ALREADY_RUNNING", "Start cannot be called when already running", "");
          lastHeartbeatTimeout = call.argument("heartbeatTimeout")!!
          val targetWidth: Int = call.argument("targetWidth")!!
          val targetHeight: Int = call.argument("targetHeight")!!
          val formatStrings: List<String>? = call.argument("formats")

          val barcodeFormats: Int = BarcodeFormats.intFromStringList(formatStrings)
          val textureEntry = textures.createSurfaceTexture()
          val reader = BarcodeReader(targetWidth, targetHeight, context, barcodeFormats,
                  this, this, textureEntry.surfaceTexture())
          readingInstance = ReadingInstance(reader, textureEntry, result)
          try {
            lastHeartbeatTimeout = lastHeartbeatTimeout?: 0
            reader.start(
                    lastHeartbeatTimeout!!
            )
          } catch (e: IOException) {
            e.printStackTrace()
            result.error("IOException", "Error starting camera because of IOException: " + e.localizedMessage, null)
          } catch (e: BarcodeReader.Exception) {
            e.printStackTrace()
            result.error(e.reason().name, "Error starting camera for reason: " + e.reason().name, null)
          } catch (e: NoPermissionException) {
            waitingForPermissionResult = true
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
              ActivityCompat.requestPermissions(activity,arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION) }
          }
        }
      }
      else if(call.method=="stop") {
        if (!waitingForPermissionResult) {
          stopReader()
        }
        result.success(null)
      }
    else if(call.method=="heartbeat"){
        readingInstance.reader.heartBeat()
        result.success(null)
      }
      else{
      result.notImplemented()
    }
  }
  private fun stopReader() {
    readingInstance.reader.stop()
    readingInstance.textureEntry.release()
    lastHeartbeatTimeout = null
  }
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
    if (requestCode == REQUEST_PERMISSION) {
      waitingForPermissionResult = false
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "Permissions request granted.")
        stopReader()
      } else {
        Log.i(TAG, "Permissions request denied.")
        permissionDenied = true
        startingFailed(BarcodeReader.Exception(BarcodeReader.Exception.Reason.noPermissions))
        stopReader()
      }
      return true
    }
    return false
  }

  override fun started() {
    val response: MutableMap<String, Any> = HashMap()
    response["surfaceWidth"] = readingInstance.reader.barCamera.getWidth()
    response["surfaceHeight"] = readingInstance.reader.barCamera.getHeight()
    response["surfaceOrientation"] = readingInstance.reader.barCamera.getOrientation()
    response["textureId"] = readingInstance.textureEntry.id()
    readingInstance.startResult.success(response)
  }

  private fun stackTraceAsString(stackTrace: Array<StackTraceElement>?): List<String>? {
    if (stackTrace == null) {
      return null
    }
    val stackTraceStrings: MutableList<String> = ArrayList(stackTrace.size)
    for (el in stackTrace) {
      stackTraceStrings.add(el.toString())
    }
    return stackTraceStrings
  }

  override fun startingFailed(t: Throwable) {
    Log.w(TAG, "Starting Flutter Qr Scanner failed", t)
    val stackTraceStrings = stackTraceAsString(t.stackTrace)
    if (t is BarcodeReader.Exception) {
      val qrException: BarcodeReader.Exception = t
      readingInstance.startResult.error("QRREADER_ERROR", qrException.reason().name, stackTraceStrings)
    } else {
      readingInstance.startResult.error("UNKNOWN_ERROR", t.message, stackTraceStrings)
    }
  }
  override fun barcodeRead(data: String, type: String) {
    val methodReturn: List<String> = listOf(data, type)
    Log.i("JB:barcodesc", "Item send: $data   $type")
    channel.invokeMethod("qrRead", methodReturn)
  }

  private class ReadingInstance(reader: BarcodeReader, textureEntry: SurfaceTextureEntry, startResult: Result) {
    val reader: BarcodeReader = reader
    val textureEntry: SurfaceTextureEntry = textureEntry
    val startResult: Result = startResult
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    this.activity = binding.activity
    binding.addRequestPermissionsResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivity() {
  }
}
