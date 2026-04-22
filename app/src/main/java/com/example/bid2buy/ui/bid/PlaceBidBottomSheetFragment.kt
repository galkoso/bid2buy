package com.example.bid2buy.ui.bid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.bid2buy.databinding.BottomSheetPlaceBidBinding
import com.example.bid2buy.repositories.BidsRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class PlaceBidBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlaceBidBinding? = null
    private val binding get() = _binding!!

    private val repository = BidsRepository()
    
    private var listingId: String = ""
    private var currentHighestBid: Double = 0.0
    private var startingPrice: Double = 0.0
    private var bidCount: Int = 0

    companion object {
        const val TAG = "PlaceBidBottomSheetFragment"
        fun newInstance(listingId: String, currentHighestBid: Double, startingPrice: Double, bidCount: Int): PlaceBidBottomSheetFragment {
            val fragment = PlaceBidBottomSheetFragment()
            val args = Bundle()
            args.putString("listingId", listingId)
            args.putDouble("currentHighestBid", currentHighestBid)
            args.putDouble("startingPrice", startingPrice)
            args.putInt("bidCount", bidCount)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listingId = it.getString("listingId", "")
            currentHighestBid = it.getDouble("currentHighestBid", 0.0)
            startingPrice = it.getDouble("startingPrice", 0.0)
            bidCount = it.getInt("bidCount", 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPlaceBidBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val minBid = if (bidCount == 0) startingPrice else currentHighestBid + 1.0
        
        binding.textCurrentHighestBid.text = "₪${if (bidCount == 0) startingPrice.toInt() else currentHighestBid.toInt()}"
        binding.bidLabel.text = if (bidCount == 0) "Starting price" else "Current highest bid"
        
        // Fix overlap by setting hint on the Layout and clearing it from the EditText
        binding.bidInputLayout.hint = "Amount (Minimum ₪${minBid.toInt()})"
        binding.editBidAmount.hint = null

        binding.btnClose.setOnClickListener { dismiss() }

        binding.editBidAmount.doAfterTextChanged { 
            binding.bidInputLayout.error = null
        }

        binding.btnConfirmBid.setOnClickListener {
            val amountStr = binding.editBidAmount.text.toString()
            val amount = amountStr.toDoubleOrNull()

            if (amount == null) {
                binding.bidInputLayout.error = "Please enter a valid amount"
                return@setOnClickListener
            }

            if (amount < minBid) {
                binding.bidInputLayout.error = "Bid must be at least ₪${minBid.toInt()}"
                return@setOnClickListener
            }

            placeBid(amount)
        }
    }

    private fun placeBid(amount: Double) {
        binding.btnConfirmBid.isEnabled = false
        lifecycleScope.launch {
            try {
                repository.placeBid(listingId, amount)
                Toast.makeText(requireContext(), "Bid placed successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.setFragmentResult("bid_placed", Bundle().apply { putBoolean("success", true) })
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnConfirmBid.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
