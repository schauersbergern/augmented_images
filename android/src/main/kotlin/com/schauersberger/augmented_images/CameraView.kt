package com.schauersberger.augmented_images

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Pair
import android.view.View
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.exceptions.*
import com.schauersberger.augmented_images.callback.CameraViewLifecycle
import com.schauersberger.augmented_images.callback.CameraViewLifecycleCallback
import com.schauersberger.augmented_images.communication.MessageStreamHandler
import com.schauersberger.augmented_images.helpers.*
import io.flutter.plugin.platform.PlatformView
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.schauersberger.augmented_images.rendering.AugmentedImageRenderer
import com.schauersberger.augmented_images.rendering.BackgroundRenderer
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import java.lang.Exception
import java.util.HashMap

class CameraView(private val activity: Activity, private val context: Context, private val binding: FlutterPlugin.FlutterPluginBinding, id: Int, creationParams: Map<String?, Any?>?) : CameraViewLifecycle, PlatformView, GLSurfaceView.Renderer {
    private val surfaceView: GLSurfaceView = GLSurfaceView(context)

    private val backgroundRenderer = BackgroundRenderer()
    private val augmentedImageRenderer = AugmentedImageRenderer()

    private val displayRotationHelper = DisplayRotationHelper(context)
    private var session: Session? = null

    private val augmentedImageMap: HashMap<Int, Pair<AugmentedImage, Anchor>> = HashMap()

    private val trackingStateHelper: TrackingStateHelper = TrackingStateHelper(activity)
    private var installRequested = false

    private val messageStreamHandler = MessageStreamHandler()
    private val triggerImages: List<String>? = creationParams?.get("triggerImagePaths") as List<String>?

    companion object {
        const val TAG = "CameraView"
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
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.

        surfaceView.setRenderer(this)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surfaceView.setWillNotDraw(false)

        initStream()
        setupLifeCycle()
    }

    private fun initStream() {
        val messageEventChannel = EventChannel(binding.binaryMessenger, EVENT_CHANNEL_NAME)
        messageEventChannel.setStreamHandler(messageStreamHandler)
    }

    private fun setupLifeCycle() {
        activity.application.registerActivityLifecycleCallbacks(
            CameraViewLifecycleCallback(this)
        )
        onResume()
    }

    override fun onResume() {
        setUpSession()
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available. Try restarting the app.")
            session = null
            return
        }
        surfaceView.onResume()
        displayRotationHelper.onResume()
    }

    private fun setUpSession() {
        when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
            InstallStatus.INSTALL_REQUESTED -> {
                installRequested = true
                return
            }
            InstallStatus.INSTALLED -> {}
        }
        if (!CameraPermissionHelper.hasCameraPermission(activity)) {
            CameraPermissionHelper.requestCameraPermission(activity)
            return
        }

        if (session == null) {
            session = Session( context )
            val config = Config(session).apply {
                focusMode = Config.FocusMode.AUTO
                setupAugmentedImageDatabase(this)
            }
            session?.configure(config)
        }
    }

    override fun onPause() {
        session?.apply {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause()
            surfaceView.onPause()
            pause()
        }
    }

    override fun onDestroy() {
        session?.apply {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            close()
            session = null
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread( context )
            augmentedImageRenderer.createOnGlThread( context )
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
            trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

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

                    messageStreamHandler.send(EVENT_CHANNEL_NAME, "image_detected", "{'image_index' : ${augmentedImage.index}}")
                }
                TrackingState.TRACKING -> {
                    // Have to switch to UI Thread to update View.
                    activity.runOnUiThread {
                        if (!augmentedImageMap.containsKey(augmentedImage.index)) {
                            val centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.centerPose)
                            augmentedImageMap[augmentedImage.index] = Pair.create(augmentedImage, centerPoseAnchor)
                        }
                    }
                }
                TrackingState.STOPPED -> augmentedImageMap.remove(augmentedImage.index)
                else -> {}
            }
        }

        // Draw all images in augmentedImageMap
        for (pair in augmentedImageMap.values) {
            val augmentedImage = pair.first
            val centerAnchor = augmentedImageMap[augmentedImage.index]!!.second
            when (augmentedImage.trackingState) {
                TrackingState.TRACKING -> augmentedImageRenderer.draw(
                    viewmtx, projmtx, augmentedImage, centerAnchor, colorCorrectionRgba
                )
                else -> {}
            }
        }
    }
    private fun setupAugmentedImageDatabase(config: Config) {

        val augmentedImageDatabase = AugmentedImageDatabase(session)
        triggerImages?.forEach {

            val path = binding.flutterAssets.getAssetFilePathByName(it)
            val stream = binding.applicationContext.assets.open(path)
            val bitmap = BitmapFactory.decodeStream(stream)

            //TODO: Performance: Third parameter image width for faster detection
            augmentedImageDatabase.addImage(path, bitmap)
        }
        config.augmentedImageDatabase = augmentedImageDatabase
    }
}

