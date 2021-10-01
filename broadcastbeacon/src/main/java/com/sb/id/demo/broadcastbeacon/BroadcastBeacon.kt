package com.sb.id.demo.broadcastbeacon

import android.app.Activity
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.util.Log
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter

class BroadcastBeacon {
    interface Callback {
        fun onSuccess()
        fun onFailed()
        fun onStopBroadcast()
    }

    companion object Builder {
        val iBeaconLayout = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
        val iBeaconManufacturer = 0x004C
        private var beacon = Beacon.Builder().build()
        private var beaconParser = BeaconParser().setBeaconLayout(iBeaconLayout)
        private var beaconTransmitter: BeaconTransmitter? = null
        private var callback: Callback? = null

        var TAG = "Broadcast_Beacon"

        var MAYOR_INTENT = "MAYOR_INTENT"
        var MINOR_INTENT = "MINOR_INTENT"
        var UUIDIBeacon_INTENT = "UUIDIBeacon_INTENT"
        var REFRESHRATE_INTENT = "REFRESHRATE_INTENT"
        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
        const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"

        //Default Broadcast 3hz
        val BROADCAST_1Hz_MODE_LOW_LATENCY = 0    //approx 1 Hz
        val BROADCAST_3Hz_MODE_BALANCED = 1    //approx 3 Hz
        val BROADCAST_10Hz_MODE_LOW_POWER = 2    //approx 10 Hz

        var mayorNow = "0"
        var minorNow = "0"
        var UuidIbeacon = "0"

        fun BroadcastBeacon(
            context: Context,
            UUID: String,
            major: String,
            minor: String,
            callback: Callback
        ) {
            this.callback = callback

            beacon = Beacon.Builder().setId1(UUID).setId2(major).setId3(minor)
                .setManufacturer(iBeaconManufacturer).setTxPower(-59)
                .setDataFields(listOf(0L))
                .build()
            beaconTransmitter = BeaconTransmitter(context, beaconParser)

            mayorNow = major
            minorNow = minor
            UuidIbeacon = UUID
        }

        fun startBroadcastBeacon() {
            stopBroadcastBeacon()

            beaconTransmitter!!.startAdvertising(beacon, object : AdvertiseCallback() {
                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)
                    callback?.onFailed()
                    Log.i(TAG, "Advertisement start failed.")

                }

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    super.onStartSuccess(settingsInEffect)
                    callback?.onSuccess()
                    Log.i(TAG, "Advertisement start succeeded.")
                }
            })
        }

        fun startBroadcastBeacon(
            refreshRate: Int
        ) {
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
            }
            startBroadcastBeacon()
        }


        fun stopBroadcastBeacon() {
            if (beaconTransmitter != null) {
                beaconTransmitter!!.stopAdvertising()
                callback?.onStopBroadcast()
            }
        }

        fun startBeaconForeground(context: Context) {
            context.startService(Intent(context, BackgroundServiceBeacon::class.java).apply {
                this.action = ACTION_START_FOREGROUND_SERVICE
                this.putExtra(MAYOR_INTENT, mayorNow)
                this.putExtra(MINOR_INTENT, minorNow)
                this.putExtra(REFRESHRATE_INTENT, BROADCAST_3Hz_MODE_BALANCED)
                this.putExtra(UUIDIBeacon_INTENT, UuidIbeacon)
            })
        }

        fun startBeaconForeground(context: Context, refreshRate: Int) {
            context.startService(Intent(context, BackgroundServiceBeacon::class.java).apply {
                this.action = ACTION_START_FOREGROUND_SERVICE
                this.putExtra(MAYOR_INTENT, mayorNow)
                this.putExtra(MINOR_INTENT, minorNow)
                this.putExtra(REFRESHRATE_INTENT, refreshRate)
                this.putExtra(UUIDIBeacon_INTENT, UuidIbeacon)
            })
        }

        fun stopBeaconForeground(context: Context) {
            context.startService(Intent(context, BackgroundServiceBeacon::class.java).apply {
                this.action = ACTION_STOP_FOREGROUND_SERVICE
            })
        }

        fun initStopServiceBackground(activity: Activity) {
            if (activity.intent.extras != null) {
                val intent = activity.intent.getStringExtra(ACTION_STOP_FOREGROUND_SERVICE)
                if (intent == ACTION_STOP_FOREGROUND_SERVICE) {
                    stopBroadcastBeacon()
                    stopBeaconForeground(activity)
                }
            }
        }

    }
}
