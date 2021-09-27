package com.sb.id.demo.broadcastbeacon

import android.app.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.ACTION_START_FOREGROUND_SERVICE
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.ACTION_START_SERVICE
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.ACTION_STOP_FOREGROUND_SERVICE
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.ACTION_STOP_SERVICE
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.MAYOR_INTENT
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.MINOR_INTENT
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.UUIDIBeacon_INTENT
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.iBeaconLayout
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter

class BackgroundServiceBeacon : Service() {
    companion object {
        private val NOTIFICATION_ID = 123
        private val CHANNEL_ID = "BroadcastBeacon"
        private lateinit var notification: Notification
        var notificationTitle = "Broadcast Beacon Active"
        var notificationText = "Looking for beacon scanner..."
        private var beaconTransmitter: BeaconTransmitter? = null
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val beaconParser =
            BeaconParser().setBeaconLayout(iBeaconLayout)
        if (beaconTransmitter != null) {
            beaconTransmitter!!.stopAdvertising()
        }
        beaconTransmitter = BeaconTransmitter(this, beaconParser)
        intent?.let {
            when (it.action) {
                ACTION_START_SERVICE -> {
                    Log.i("Main_Activity", "On Service.")
                    startBroadcastBeacon(intent)
                }
                ACTION_STOP_SERVICE -> {
                    beaconTransmitter!!.stopAdvertising()
                    stopSelf()
                    Toast.makeText(
                        this@BackgroundServiceBeacon,
                        "Beacon Tidak Aktif",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                ACTION_START_FOREGROUND_SERVICE -> {
                    startForeground(NOTIFICATION_ID, setNotification(applicationContext))
                    startBroadcastBeacon(intent)

                }
                ACTION_STOP_FOREGROUND_SERVICE -> {
                    Log.e(
                        "Broadcast_Beacon",
                        "Stop Foreground"
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else stopForeground(true)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun startBroadcastBeacon(intent: Intent) {


        val mayor = intent.getStringExtra(MAYOR_INTENT)
        val minor = intent.getStringExtra(MINOR_INTENT)
        val UUIdIBeacon = intent.getStringExtra(UUIDIBeacon_INTENT)

        val beacon = Beacon.Builder().setId1(UUIdIBeacon)
            .setId2(mayor) //major
            .setId3(minor) //minor
            .setManufacturer(0x004C) //for iBeacon
            .setTxPower(-59)
            .setDataFields(listOf(0L))
            .build()

//                    if (REFRESHRATE!="REFRESHRATE") {
//                        beaconTransmitter.advertiseMode =
//                            intent.getIntExtra(REFRESHRATE, 0)
//                    }
        Log.v(
            "Broadcast_Beacon",
            "After change hz : " + beaconTransmitter!!.advertiseMode.toString()
        )

        //       ADVERTISE_MODE_LOW_LATENCY	approx 1 Hz
        //       ADVERTISE_MODE_BALANCED	approx 3 Hz
        //       ADVERTISE_MODE_LOW_POWER	approx 10 Hz
        beaconTransmitter!!.startAdvertising(beacon, object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Toast.makeText(
                    this@BackgroundServiceBeacon,
                    "Beacon Tidak Aktif",
                    Toast.LENGTH_LONG
                )
                    .show()
                Log.e(
                    "Broadcast_Beacon",
                    "Advertisement start failed with code: $errorCode"
                )
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Toast.makeText(
                    this@BackgroundServiceBeacon,
                    "Beacon Aktif",
                    Toast.LENGTH_LONG
                )
                    .show()
                Log.i("Broadcast_Beacon", "Advertisement start succeeded.")
            }
        })
    }

    private fun setNotification(context: Context): Notification {
        val notificationManager: NotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "Broadcast Beacon Notifications",
                NotificationManager.IMPORTANCE_MIN
            )

            //Configure notification channel
            notificationChannel.description = (context.packageName)
            notificationChannel.setSound(null, null)
            notificationChannel.enableLights(false)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(false)

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val packageManager = context.packageManager
        //opem app on click notification
        val resultIntent = packageManager.getLaunchIntentForPackage(context.packageName)
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addNextIntent(resultIntent)

        //Get The pending intent containing the entire back stack
        val broadcastIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        //disable viration
        //disable vibration
        val intent = Intent(context, BackgroundServiceBeacon::class.java)
        intent.setAction(ACTION_STOP_FOREGROUND_SERVICE)
        val stopPendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)


        val pattern = longArrayOf(0L)
        notification = notificationBuilder.setTicker("New Ticker Message")
            .setSmallIcon(R.drawable.ic_baseline_bluetooth_audio_24)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSound(null)
            //hide from notification bar
            .setPriority(Notification.PRIORITY_MIN)
            .setVibrate(pattern)
            .setContentInfo("Content Info")
            .setContentIntent(broadcastIntent)
            .addAction(R.drawable.ic_baseline_bluetooth_audio_24, "Stop", stopPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        return notification

    }
}