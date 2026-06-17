package com.callify.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.callify.databinding.FragmentDialpadBinding

/**
 * Fragment hosting the numeric dial pad.
 *
 * Digit keys are vertical LinearLayouts (number + optional letter label).
 * They share the same [setOnClickListener] pattern via a map because
 * all keys have the same parent type [View].
 *
 * Display behaviour:
 * - Shows "|" (cursor) when empty.
 * - Shows the typed number otherwise.
 * - Backspace removes the last digit and restores "|" when the field is empty.
 */
class DialPadFragment : Fragment() {

    private var _binding: FragmentDialpadBinding? = null
    private val binding get() = _binding!!

    private val numberBuilder = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialpadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wireDigitButtons()

        binding.btnBackspace.setOnClickListener {
            if (numberBuilder.isNotEmpty()) {
                numberBuilder.deleteCharAt(numberBuilder.lastIndex)
                if (numberBuilder.isEmpty()) {
                    binding.tvDialDisplay.text = ""
                    binding.btnBackspace.visibility = View.GONE
                } else {
                    binding.tvDialDisplay.text = "${numberBuilder.toString()}|"
                }
            }
        }

        binding.btnCall.setOnClickListener {
            val number = numberBuilder.toString()
            if (number.isNotEmpty()) {
                dialNumber(number)
            } else {
                Toast.makeText(requireContext(), "Enter a number to dial", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun wireDigitButtons() {
        mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btnStar to "*", binding.btnHash to "#"
        ).forEach { (btn, digit) ->
            btn.setOnClickListener {
                if (numberBuilder.isEmpty()) {
                    binding.btnBackspace.visibility = View.VISIBLE
                }
                numberBuilder.append(digit)
                binding.tvDialDisplay.text = "${numberBuilder.toString()}|"
            }
        }
    }

    private fun dialNumber(number: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CALL_PHONE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Call permission required", Toast.LENGTH_SHORT).show()
            return
        }
        val telecom = requireContext().getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        telecom.placeCall(Uri.fromParts("tel", number, null), null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
