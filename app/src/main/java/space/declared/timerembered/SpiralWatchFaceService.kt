package space.declared.timerembered

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class SpiralWatchFaceService: Service() {
    private val TAG = "SpiralWatchFaceService";

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SpiralWatchFaceService onStartCommand");
        return START_NOT_STICKY;
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SpiralWatchFaceService onCreate");
    }

    @Override
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SpiralWatchFaceService onDestroy");
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}