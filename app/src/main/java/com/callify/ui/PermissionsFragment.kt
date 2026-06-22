package com.callify.ui

import android.Manifest
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.callify.databinding.FragmentPermissionsBinding
import com.callify.utils.PermissionHelper

/**
 * Fragment hosting the Callify permission dashboard.
 *
 * Contains the About section and three permission toggle rows
 * (Phone State, Notifications, Draw Over Apps).
 * Mirrors the original [com.callify.MainActivity] permission UI exactly.
 */
class PermissionsFragment : Fragment() {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.aboutLabel.paintFlags =
            binding.aboutLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.permissionsLabel.paintFlags =
            binding.permissionsLabel.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        wirePermissionRowClicks()
        refreshPermissionStates()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    /** Re-reads grant states and updates the SwitchCompat toggles. */
    fun refreshPermissionStates() {
        if (_binding == null) return
        binding.switchPhoneState.isChecked    = PermissionHelper.hasPhoneStatePermission(requireContext())
        binding.switchNotifications.isChecked = PermissionHelper.hasNotificationPermission(requireContext())
        binding.switchOverlay.isChecked       = PermissionHelper.hasOverlayPermission(requireContext())
        binding.switchPhoneNumbers.isChecked  = PermissionHelper.hasPhoneNumbersPermission(requireContext())
    }

    private fun wirePermissionRowClicks() {
        binding.rowPhoneState.setOnClickListener {
            if (!PermissionHelper.hasPhoneStatePermission(requireContext())) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG),
                    REQUEST_CODE_PHONE_STATE
                )
            }
        }
        binding.rowNotifications.setOnClickListener {
            if (!PermissionHelper.hasNotificationPermission(requireContext())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_CODE_NOTIFICATION
                    )
                }
            }
        }
        binding.rowOverlay.setOnClickListener {
            if (!PermissionHelper.hasOverlayPermission(requireContext())) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${requireContext().packageName}")
                    )
                )
            }
        }
        binding.rowPhoneNumbers.setOnClickListener {
            if (!PermissionHelper.hasPhoneNumbersPermission(requireContext())) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_PHONE_NUMBERS),
                    REQUEST_CODE_PHONE_NUMBERS
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PHONE_STATE   = 1001
        private const val REQUEST_CODE_NOTIFICATION  = 1002
        private const val REQUEST_CODE_PHONE_NUMBERS = 1003
    }
}
