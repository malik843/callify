package com.callify.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
 * Allows the user to type a phone number and initiate an outgoing call via
 * [TelecomManager.placeCall]. No call history is read or written anywhere in this fragment.
 */
class DialPadFragment : Fragment() {

    private var _binding: FragmentDialpadBinding? = null
    private val binding get() = _binding!!

    /** Accumulates typed digits. Cleared on view destroy. */
    private val numberBuilder = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
                binding.tvDialDisplay.text = numberBuilder.toString()
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

    /**
     * Maps each digit/symbol button to its corresponding character
     * and appends it to [numberBuilder] on click.
     */
    private fun wireDigitButtons() {
        val buttonMap = mapOf(
            binding.btn0 to "0",
            binding.btn1 to "1",
            binding.btn2 to "2",
            binding.btn3 to "3",
            binding.btn4 to "4",
            binding.btn5 to "5",
            binding.btn6 to "6",
            binding.btn7 to "7",
            binding.btn8 to "8",
            binding.btn9 to "9",
            binding.btnStar to "*",
            binding.btnHash to "#"
        )
        buttonMap.forEach { (btn, digit) ->
            btn.setOnClickListener {
                numberBuilder.append(digit)
                binding.tvDialDisplay.text = numberBuilder.toString()
            }
        }
    }

    /**
     * Places an outgoing call via [TelecomManager].
     *
     * Checks [Manifest.permission.CALL_PHONE] before proceeding.
     * Emergency dialing is handled automatically by the system — this function
     * does not special-case or intercept emergency numbers.
     *
     * No call record is written by this function.
     *
     * @param number The dialled number string (not normalised — passed as-is to Telecom).
     */
    private fun dialNumber(number: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                requireContext(),
                "Phone call permission is required to dial",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val telecomManager =
            requireContext().getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val uri = Uri.fromParts("tel", number, null)
        telecomManager.placeCall(uri, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
