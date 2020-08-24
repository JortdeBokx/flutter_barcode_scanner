package com.github.jortdebokx.flutter_barcode_scanner
import android.os.Handler;

class Heartbeat(private val timeout: Int, private val runner: Runnable) {
    private val handler: Handler = Handler()
    fun beat() {
        handler.removeCallbacks(runner)
        handler.postDelayed(runner, timeout.toLong())
    }

    fun stop() {
        handler.removeCallbacks(runner)
    }

    init {
        handler.postDelayed(runner, timeout.toLong())
    }
}
