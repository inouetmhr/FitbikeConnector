package org.noue.fitbikeconnector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException

class UsbReaderWorker (context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object{
        val TAG = "UsbReaderWorker"
        val title  = "Reading USB Serial device"
        val notificationId = TAG
        var sInstance : UsbReaderWorker? = null
    }
    private val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        //inputData.getString("aa")
        Log.d(TAG, "doWork() $this")
        sInstance = this

        // Mark the Worker as important
        setForeground(createForegroundInfo())
        reading()
        //dummy_forDebug()
        return Result.success()
    }

    private fun dummy_forDebug(){
        repeat(10){
            Thread.sleep(1000)
        }
    }

    private fun reading() {
        Log.i(TAG, "USB read thread start")
        var activity = MainActivity.sInstance
        var msg: String
        var rpm: Double
        val rpms = MutableList(5) { 0.0 }
        var distance = 0
        //var unread_count = 0
        var updFlg = false
        try {
            while (!this.isStopped) {
                activity = MainActivity.sInstance
                val buffer = ByteArray(16)
                val numBytesRead: Int = activity.port.read(buffer, 1000)
                //Log.v(TAG, "read $numBytesRead")
                if (numBytesRead > 0) {
                    msg = String(buffer, 0, numBytesRead)
                    Log.v(TAG, "read from usb serial:${msg}")
                    rpm = msg.lines()[0].toDouble()
                    distance += 3
                    updFlg = true
                } else { //unread_count += 1
                    rpm = 0.0
                }
                rpms.add(rpm)
                rpms.removeAt(0)
                rpm = rpms.average()

                if (distance % 15 == 0 && updFlg) {
                    updFlg = false
                    activity.uploadToFirestore(distance.toDouble())
                }
                activity.updateMainFragment(rpm)
            }
        } catch (e: IOException) {  //USB外れた場合
            Log.i(TAG, "USB serial disconnected")
            activity.runOnUiThread{
                Toast.makeText(
                    activity.applicationContext,
                    "Serial device disconnected",
                    Toast.LENGTH_LONG
                ).show() }
            if (activity.port.isOpen) activity.port.close()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            Log.w(TAG, "thread interrupted/ terminating")
            e.printStackTrace()
        }
        activity.updateMainFragment(0.0)
    }

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    private fun createForegroundInfo(): ForegroundInfo {
        //val id = applicationContext.getString(R.string.notification_channel_id)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)
        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val notification = NotificationCompat.Builder(applicationContext, notificationId)
            .setContentTitle(title)
            //.setContentText("description")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, "Stop", intent)
            .build()
        return ForegroundInfo(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val chan = NotificationChannel(notificationId,
            title, NotificationManager.IMPORTANCE_NONE)
        //chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(chan)
    }

}