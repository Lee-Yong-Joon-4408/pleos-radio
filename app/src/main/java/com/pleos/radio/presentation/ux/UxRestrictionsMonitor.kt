package com.pleos.radio.presentation.ux

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * Watches the car's driver-distraction restrictions. [onVideoAllowedChanged] receives false while the
 * system forbids video (UX_RESTRICTIONS_NO_VIDEO, i.e. driving), true when parked. Non-automotive devices
 * always allow video. On Android Automotive, video stays blocked until the restriction service answers.
 */
class UxRestrictionsMonitor(
    private val context: Context,
    private val onVideoAllowedChanged: (Boolean) -> Unit,
) {
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    fun start() {
        val automotive = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        if (!automotive || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onVideoAllowedChanged(true)
            return
        }
        onVideoAllowedChanged(false)
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (!ready) return@createCar
                val m = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                    ?: return@createCar
                manager = m
                report(m.currentCarUxRestrictions)
                m.registerListener { report(it) }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "UX restriction service unavailable; video stays off", t)
        }
    }

    private fun report(restrictions: CarUxRestrictions?) {
        val noVideo = restrictions != null && restrictions.isRequiresDistractionOptimization &&
            (restrictions.activeRestrictions and CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO) != 0
        onVideoAllowedChanged(!noVideo)
    }

    fun stop() {
        runCatching { manager?.unregisterListener() }
        runCatching { car?.disconnect() }
        manager = null
        car = null
    }

    private companion object {
        const val TAG = "UxRestrictions"
    }
}
