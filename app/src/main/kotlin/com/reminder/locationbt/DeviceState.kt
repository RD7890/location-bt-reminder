package com.reminder.locationbt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.LocationManager

/** Pure utility object — no state, no context leak. */
object DeviceState {

    /** Returns true if the system Location provider (GPS or Network) is enabled. */
    fun isLocationOn(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /** Returns true if the Bluetooth adapter is enabled. */
    fun isBluetoothOn(context: Context): Boolean {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bm?.adapter?.isEnabled == true
    }

    /** Returns true when Location is off (reminder loop should stop). */
    fun isLocationOff(context: Context): Boolean = !isLocationOn(context)
}
