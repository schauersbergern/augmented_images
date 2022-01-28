package com.schauersberger.augmented_images.callback

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.schauersberger.augmented_images.CameraView

interface CameraViewLifecycle {
    fun onResume()
    fun onPause()
    fun onDestroy()
}

class CameraViewLifecycleCallback(private val cycle: CameraViewLifecycle) : Application.ActivityLifecycleCallbacks{
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
        Log.d(CameraView.TAG, "onActivityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d(CameraView.TAG, "onActivityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d(CameraView.TAG, "onActivityResumed")
        cycle.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(CameraView.TAG, "onActivityPaused")
        cycle.onPause()
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d(CameraView.TAG, "onActivityStopped")
        // onStopped()
        cycle.onPause()
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {}

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(CameraView.TAG, "onActivityDestroyed")
//                        onPause()
//                        onDestroy()
    }

}