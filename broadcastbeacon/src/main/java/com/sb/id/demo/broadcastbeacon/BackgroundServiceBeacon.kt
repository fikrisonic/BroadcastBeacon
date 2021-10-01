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
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.ACTION_STOP_FOREGROUND_SERVICE
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.BROADCAST_3Hz_MODE_BALANCED
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.MAYOR_INTENT
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.MINOR_INTENT
import com.sb.id.demo.broadcastbeacon.BroadcastBeacon.Builder.TAG
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
                ACTION_START_FOREGROUND_SERVICE -> {
                    startForeground(NOTIFICATION_ID, setNotification(applicationContext))
                    startBroadcastBeacon(intent)

                }
                ACTION_STOP_FOREGROUND_SERVICE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else stopForeground(true)
                    stopSelf()
                    Log.i(TAG, "Beacon is not active")
                }
                else -> {
                    Log.i(TAG, "Invalid action service")
                }
            }
        }
        return START_STICKY
    }

    fun startBroadcastBeacon(intent: Intent) {
        val mayor = intent.getStringExtra(MAYOR_INTENT)
        val minor = intent.getStringExtra(MINOR_INTENT)
        val UUIdIBeacon = intent.getStringExtra(UUIDIBeacon_INTENT)
        val refreshRate =
            intent.getIntExtra(BroadcastBeacon.REFRESHRATE_INTENT, BROADCAST_3Hz_MODE_BALANCED)

        val beacon = Beacon.Builder().setId1(UUIdIBeacon)
            .setId2(mayor) //major
            .setId3(minor) //minor
            .setManufacturer(0x004C) //for iBeacon
            .setTxPower(-59)
            .setDataFields(listOf(0L))
            .build()

        when (refreshRate) {
            0 -> {
                beaconTransmitter!!.advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
            }
            1 -> {
                beaconTransmitter!!.advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED
            }
            2 -> {
                beaconTransmitter!!.advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
            }
            else -> {
                Log.v(
                    TAG,
                    "Refresh rate is not valid"
                )
            }
        }

        Log.v(
            TAG,
            "After change hz : " + beaconTransmitter!!.advertiseMode.toString()
        )

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
                    TAG,
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
                Log.i(TAG, "Advertisement start succeeded.")
            }
        })
    }

    fun setNotification(context: Context): Notification {
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
        val stopServiceIntent =
            packageManager.getLaunchIntentForPackage(context.packageName)!!.putExtra(
                ACTION_STOP_FOREGROUND_SERVICE, ACTION_STOP_FOREGROUND_SERVICE
            )
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addNextIntent(resultIntent)

        val stackBuilderStopBroadcast = TaskStackBuilder.create(context)
        stackBuilderStopBroadcast.addNextIntent(stopServiceIntent)

        //Get The pending intent containing the entire back stack
        val broadcastIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        val stopBroadcastIntent =
            stackBuilderStopBroadcast.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        //disable vibration
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
            .addAction(R.drawable.ic_baseline_bluetooth_audio_24, "Stop", stopBroadcastIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        return notification

    }
}