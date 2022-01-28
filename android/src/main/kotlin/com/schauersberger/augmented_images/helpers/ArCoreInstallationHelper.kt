package com.schauersberger.augmented_images.helpers

import android.app.Activity
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.lang.Exception

object ArCoreInstallationHelper {

    const val TAG = "ArCoreInstallationHelper"

    fun getArcoreInstallStatusAndInstall(activity: Activity) : Boolean{
        var exception: Exception?
        var message: String?
        try {
            return when (ArCoreApk.getInstance().requestInstall(activity, true)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    false
                }
                ArCoreApk.InstallStatus.INSTALLED -> {
                    true
                }
            }
        } catch (e: UnavailableArcoreNotInstalledException) {
            message = "Please install ARCore"
            exception = e
        } catch (e: UnavailableUserDeclinedInstallationException) {
            message = "Please install ARCore"
            exception = e
        } catch (e: UnavailableApkTooOldException) {
            message = "Please update ARCore"
            exception = e
        } catch (e: UnavailableSdkTooOldException) {
            message = "Please update this app"
            exception = e
        } catch (e: Exception) {
            message = "This device does not support AR"
            exception = e
        }
        if (message != null) {
            Log.e(
                TAG,
                "Exception creating session",
                exception
            )
            return false
        }
        return false
    }
}