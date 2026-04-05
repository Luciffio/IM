package com.example.im

import android.app.Application
import android.util.Log
import java.io.File

class IMApplication : Application() {

    private var backendProcess: Process? = null

    override fun onCreate() {
        super.onCreate()
        startBackend()
    }

    private fun startBackend() {
        val binary = File(applicationInfo.nativeLibraryDir, "libbackend.so")
        if (!binary.exists()) {
            Log.e(TAG, "Backend binary not found: ${binary.absolutePath}")
            return
        }

        val sessionDir = File(filesDir, "tg_session").also { it.mkdirs() }

        try {
            backendProcess = ProcessBuilder(binary.absolutePath)
                .directory(filesDir)
                .apply {
                    environment().apply {
                        put("TG_APP_ID",      BuildConfig.TG_APP_ID)
                        put("TG_APP_HASH",    BuildConfig.TG_APP_HASH)
                        put("TG_SESSION_DIR", sessionDir.absolutePath)
                        put("HOME",           filesDir.absolutePath)
                    }
                }
                .redirectErrorStream(true)   // merge stderr → stdout
                .start()

            // Stream backend output to logcat on a daemon thread.
            val proc = backendProcess!!
            Thread {
                proc.inputStream.bufferedReader().forEachLine { line ->
                    Log.d(TAG, line)
                }
            }.also { it.isDaemon = true; it.start() }

            Log.i(TAG, "Backend started (${binary.absolutePath})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start backend", e)
        }
    }

    override fun onTerminate() {
        backendProcess?.destroy()
        super.onTerminate()
    }

    companion object {
        private const val TAG = "IMBackend"
    }
}
