package com.schauersberger.augmented_images

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.TextView
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.exceptions.*
import com.schauersberger.augmented_images.helpers.CameraPermissionHelper
import io.flutter.plugin.platform.PlatformView
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.schauersberger.augmented_images.helpers.DisplayRotationHelper
import com.schauersberger.augmented_images.rendering.AugmentedImageRenderer
import com.schauersberger.augmented_images.rendering.BackgroundRenderer
import io.flutter.plugin.common.BinaryMessenger
import java.lang.Exception
import java.util.HashMap

class CameraView(private val activity: Activity, private val context: Context, messenger: BinaryMessenger, id: Int, creationParams: Map<String?, Any?>?) : PlatformView, GLSurfaceView.Renderer {
    private val textView: TextView = TextView(context)
    private val surfaceView: GLSurfaceView = GLSurfaceView(context)

    private val backgroundRenderer = BackgroundRenderer()
    private val augmentedImageRenderer = AugmentedImageRenderer()

    private val displayRotationHelper = DisplayRotationHelper(context)
    private var session: Session? = null


    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    private val augmentedImageMap: Map<Int, Pair<AugmentedImage, Anchor>> = HashMap()

    //private val trackingStateHelper: TrackingStateHelper = TrackingStateHelper(this)

    private var installRequested = false
    private var shouldConfigureSession = false

    companion object {
        private const val TAG = "CameraView"
    }

    override fun getView(): View {
        return surfaceView
    }

    override fun dispose() {
        // Destroy AR session
        Log.d(TAG, "dispose called")
        try {
            onPause()
            onDestroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        setupLifeCycle(context)

        // Set up renderer.
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.

        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surfaceView.setWillNotDraw(false)


        textView.textSize = 72f
        textView.setBackgroundColor(Color.rgb(255, 255, 255))
        textView.text = "Rendered on a native Android view (id: $id)"

    }

    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) {
                    Log.d(TAG, "onActivityCreated")
                }

                override fun onActivityStarted(activity: Activity) {
                    Log.d(TAG, "onActivityStarted")
                }

                override fun onActivityResumed(activity: Activity) {
                    Log.d(TAG, "onActivityResumed")
                    onResume()
                }

                override fun onActivityPaused(activity: Activity) {
                    Log.d(TAG, "onActivityPaused")
                    onPause()
                }

                override fun onActivityStopped(activity: Activity) {
                    Log.d(TAG, "onActivityStopped")
                    // onStopped()
                    onPause()
                }

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle
                ) {}

                override fun onActivityDestroyed(activity: Activity) {
                    Log.d(TAG, "onActivityDestroyed")
//                        onPause()
//                        onDestroy()
                }
            }

        activity.application.registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
    }

    fun onResume() {

        if (session == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }
                    InstallStatus.INSTALLED -> {}
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(activity)) {
                    CameraPermissionHelper.requestCameraPermission(activity)
                    return
                }
                session = Session( context)
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
                return
            }
            shouldConfigureSession = true
        }

        if (shouldConfigureSession) {
            configureSession()
            shouldConfigureSession = false
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available. Try restarting the app.")
            session = null
            return
        }
        surfaceView.onResume()
        displayRotationHelper.onResume()

        //Todo: reactivate
        //fitToScanView.setVisibility(View.VISIBLE)

    }

    private fun configureSession() {
        val config = Config(session)
        config.focusMode = Config.FocusMode.AUTO
        //Todo: Check if img database is needed
        /*if (!setupAugmentedImageDatabase(config)) {
            messageSnackbarHelper.showError(this, "Could not setup augmented image database")
        }*/
        session?.configure(config)
    }

    fun onPause() {
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause()
            surfaceView.onPause()
            session?.pause()
        }
    }

    fun onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session!!.close()
            session = null
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread( /*context=*/ context)
            augmentedImageRenderer.createOnGlThread( /*context=*/context)
        } catch (e: IOException) {
            Log.e(TAG,"Failed to read an asset file",e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen to notify driver it should not load any pixels from previous frame.

        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (session == null) {
            return
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session!!)

        try {
            session?.setCameraTextureName(backgroundRenderer.textureId)

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            val frame: Frame = session!!.update()
            val camera: Camera = frame.getCamera()

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            //TODO: Check if we need that
            //trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame)

            // Get projection matrix.
            val projmtx = FloatArray(16)
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

            // Get camera matrix and draw.
            val viewmtx = FloatArray(16)
            camera.getViewMatrix(viewmtx, 0)

            // Compute lighting from average intensity of the image.
            val colorCorrectionRgba = FloatArray(4)
            frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)

            // Visualize augmented images.
            drawAugmentedImages(frame, projmtx, viewmtx, colorCorrectionRgba)
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG,"Exception on the OpenGL thread",t)
        }
    }

    private fun drawAugmentedImages(
        frame: Frame, projmtx: FloatArray, viewmtx: FloatArray, colorCorrectionRgba: FloatArray
    ) {
        val updatedAugmentedImages = frame.getUpdatedTrackables(
            AugmentedImage::class.java
        )

        // Iterate to update augmentedImageMap, remove elements we cannot draw.
        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    val text = String.format("Detected Image %d", augmentedImage.index)
                    Log.i(TAG, text)
                }
                //TODO: Show augmented shit
                /*TrackingState.TRACKING -> {
                    // Have to switch to UI Thread to update View.
                    this.runOnUiThread(
                        Runnable { fitToScanView!!.visibility = View.GONE })

                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage.index)) {
                        val centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.centerPose)
                        augmentedImageMap[augmentedImage.index] =
                            Pair.create(augmentedImage, centerPoseAnchor)
                    }
                }
                TrackingState.STOPPED -> augmentedImageMap.remove(augmentedImage.index)*/
                else -> {}
            }
        }

        // Draw all images in augmentedImageMap
        for (pair in augmentedImageMap.values) {
            val augmentedImage = pair!!.first
            val centerAnchor = augmentedImageMap[augmentedImage.index]!!.second
            when (augmentedImage.trackingState) {
                TrackingState.TRACKING -> augmentedImageRenderer.draw(
                    viewmtx, projmtx, augmentedImage, centerAnchor, colorCorrectionRgba
                )
                else -> {}
            }
        }
    }
}