package com.callify.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.callify.R
import com.callify.data.model.CallerResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of the floating caller identification overlay.
 * Responsible for adding and removing the overlay view from the WindowManager.
 */
@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    private val tag = "Callify"

    /**
     * Shows the loading skeleton state of the overlay.
     * Called immediately when a call is detected.
     */
    fun showLoading() {
        if (!Settings.canDrawOverlays(context)) {
            Log.e(tag, "Cannot show overlay: Permission missing")
            return
        }

        if (overlayView != null) {
            dismiss()
        }

        val themedContext = ContextThemeWrapper(context, R.style.Theme_Callify)
        val inflater = LayoutInflater.from(themedContext)
        // Passing null is acceptable for WindowManager overlays as there is no parent ViewGroup.
        overlayView = inflater.inflate(R.layout.overlay_caller, null)

        overlayView?.let { view ->
            val skeleton = view.findViewById<LinearLayout>(R.id.loadingSkeleton)
            val name = view.findViewById<TextView>(R.id.callerName)
            val address = view.findViewById<TextView>(R.id.callerAddress)
            val avatarView = view.findViewById<View>(R.id.avatarView)
            val avatarInitial = view.findViewById<TextView>(R.id.avatarInitial)
            val dismissButton = view.findViewById<ImageButton>(R.id.dismissButton)

            skeleton.visibility = View.VISIBLE
            name.visibility = View.GONE
            address.visibility = View.GONE
            avatarView.visibility = View.GONE
            avatarInitial.visibility = View.GONE

            dismissButton.setOnClickListener { dismiss() }

            try {
                windowManager.addView(view, createLayoutParams())
            } catch (e: Exception) {
                Log.e(tag, "Error adding loading overlay: ${e.message}")
                overlayView = null
            }
        }
    }

    /**
     * Updates or shows the overlay with the lookup result.
     *
     * @param result The [CallerResult] to display.
     */
    fun show(result: CallerResult) {
        if (!Settings.canDrawOverlays(context)) {
            Log.e(tag, "Cannot update overlay: Permission missing")
            return
        }

        // If not already showing (e.g. showLoading was skipped or failed), create it
        if (overlayView == null) {
            val themedContext = ContextThemeWrapper(context, R.style.Theme_Callify)
            val inflater = LayoutInflater.from(themedContext)
            overlayView = inflater.inflate(R.layout.overlay_caller, null)
            overlayView?.let { view ->
                view.findViewById<ImageButton>(R.id.dismissButton).setOnClickListener { dismiss() }
                try {
                    windowManager.addView(view, createLayoutParams())
                } catch (e: Exception) {
                    Log.e(tag, "Error adding overlay: ${e.message}")
                    overlayView = null
                    return
                }
            }
        }

        overlayView?.let { bind(it, result) }
    }

    /**
     * Binds the [CallerResult] data to the overlay view.
     */
    private fun bind(view: View, result: CallerResult) {
        val skeleton = view.findViewById<LinearLayout>(R.id.loadingSkeleton)
        val name = view.findViewById<TextView>(R.id.callerName)
        val address = view.findViewById<TextView>(R.id.callerAddress)
        val avatarView = view.findViewById<View>(R.id.avatarView)
        val avatarInitial = view.findViewById<TextView>(R.id.avatarInitial)

        skeleton.visibility = View.GONE
        name.visibility = View.VISIBLE
        address.visibility = View.VISIBLE
        avatarView.visibility = View.VISIBLE
        avatarInitial.visibility = View.VISIBLE

        when (result) {
            is CallerResult.Found -> {
                avatarInitial.text = result.info.firstname?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                val fullName = context.getString(
                    R.string.caller_name_format,
                    result.info.firstname ?: "",
                    result.info.lastname ?: ""
                ).trim()
                name.text = fullName
                val displayAddress = result.info.address ?: context.getString(R.string.address_unavailable)
                address.text = displayAddress
                
                view.contentDescription = "Incoming call from $fullName. $displayAddress"
            }
            is CallerResult.NotFound -> {
                name.text = context.getString(R.string.unknown_caller)
                address.text = context.getString(R.string.not_found_address)
                avatarInitial.text = "?"
                
                view.contentDescription = "Incoming call from unknown caller"
            }
            is CallerResult.Timeout -> {
                name.text = context.getString(R.string.timeout_name)
                address.text = context.getString(R.string.timeout_address)
                avatarInitial.text = "!"
                
                view.contentDescription = "Caller ID lookup timed out"
            }
            is CallerResult.NetworkError -> {
                name.text = context.getString(R.string.network_error_name)
                address.text = context.getString(R.string.network_error_address)
                avatarInitial.text = "!"
                
                view.contentDescription = "Caller ID unavailable, no connection"
            }
        }
    }

    /**
     * Dismisses the active overlay and removes it from the WindowManager.
     */
    fun dismiss() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(tag, "Overlay already removed or error removing: ${e.message}")
            }
            overlayView = null
        }
    }

    /**
     * Creates the LayoutParams for the overlay window.
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 48.dpToPx(context)
        }
    }

    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}
