package com.sb.id.demo.broadcastbeacon

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.util.Log
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter

class BroadcastBeacon {
    interface Callback{
        fun onSuccess()
        fun onFailed()
        fun onStopBroadcast()
    }

    companion object Builder {
        private val iBeaconLayout = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
        private val iBeaconManufacturer = 0x004C
        private var beacon = Beacon.Builder().build()
        private var beaconParser = BeaconParser().setBeaconLayout(iBeaconLayout)
        private var beaconTransmitter: BeaconTransmitter? = null
        private var callback: Callback? = null

        val ADVERTISE_1Hz_MODE_LOW_LATENCY = 0    //approx 1 Hz
        val ADVERTISE_3Hz_MODE_BALANCED = 1    //approx 3 Hz
        val ADVERTISE_10Hz_MODE_LOW_POWER = 2    //approx 10 Hz

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
        }

        fun startBroadcastBeacon() {
            beaconTransmitter!!.startAdvertising(beacon, object : AdvertiseCallback() {
                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)
                    callback?.onFailed()
                    Log.i("Broadcast_Beacon", "Advertisement start failed.")

                }

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    super.onStartSuccess(settingsInEffect)
                    callback?.onSuccess()
                    Log.i("Broadcast_Beacon", "Advertisement start succeeded.")
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

            beaconTransmitter!!.startAdvertising(beacon, object : AdvertiseCallback() {
                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)
                    Log.i("Broadcast_Beacon", "Advertisement start failed.")
                    callback?.onFailed()

                }

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    super.onStartSuccess(settingsInEffect)
                    callback?.onSuccess()

                    Log.i("Broadcast_Beacon", "Advertisement start succeeded.")
                }
            })
        }


        fun stopBroadcastBeacon() {
            beaconTransmitter!!.stopAdvertising()
            callback?.onStopBroadcast()
        }
    }
}