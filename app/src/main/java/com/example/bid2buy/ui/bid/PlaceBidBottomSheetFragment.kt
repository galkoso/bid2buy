package com.example.bid2buy.ui.bid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.databinding.BottomSheetPlaceBidBinding
import com.example.bid2buy.repositories.BidsRepository
import com.example.bid2buy.util.CurrencyManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class PlaceBidBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlaceBidBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BidsRepository
    private lateinit var currencyManager: CurrencyManager
    
    private var listingId: String = ""
    private var currentHighestBid: Double = 0.0
    private var startingPrice: Double = 0.0
    private var bidCount: Int = 0
    private var listingCurrency: String = "ILS"

    companion object {
        const val TAG = "PlaceBidBottomSheetFragment"
        fun newInstance(
            listingId: String, 
            currentHighestBid: Double, 
            startingPrice: Double, 
            bidCount: Int,
            listingCurrency: String
        ): PlaceBidBottomSheetFragment {
            val fragment = PlaceBidBottomSheetFragment()
            val args = Bundle()
            args.putString("listingId", listingId)
            args.putDouble("currentHighestBid", currentHighestBid)
            args.putDouble("startingPrice", startingPrice)
            args.putInt("bidCount", bidCount)
            args.putString("listingCurrency", listingCurrency)
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
            listingCurrency = it.getString("listingCurrency", "ILS")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPlaceBidBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext().applicationContext)
        repository = BidsRepository(database.bidDao(), database.listingDao())
        currencyManager = CurrencyManager.getInstance(requireContext())

        val selectedCurrency = currencyManager.getSelectedCurrency()
        
        val minBidInOriginalCurrency = if (bidCount == 0) startingPrice else currentHighestBid + 1.0
        
        val currentPriceInSelectedCurrency = currencyManager.convert(
            if (bidCount == 0) startingPrice else currentHighestBid,
            listingCurrency,
            selectedCurrency
        )
        val minBidInSelectedCurrency = currencyManager.convert(minBidInOriginalCurrency, listingCurrency, selectedCurrency)
        
        binding.textCurrentHighestBid.text = currencyManager.formatPrice(currentPriceInSelectedCurrency, selectedCurrency)
        binding.bidLabel.text = if (bidCount == 0) "Starting price" else "Current highest bid"
        
        binding.bidInputLayout.hint = "Amount (Minimum ${currencyManager.formatPrice(minBidInSelectedCurrency, selectedCurrency)})"
        binding.editBidAmount.hint = null

        binding.btnClose.setOnClickListener { dismiss() }

        binding.editBidAmount.doAfterTextChanged { 
            binding.bidInputLayout.error = null
        }

        binding.btnConfirmBid.setOnClickListener {
            val amountStr = binding.editBidAmount.text.toString()
            val amountInSelectedCurrency = amountStr.toDoubleOrNull()

            if (amountInSelectedCurrency == null) {
                binding.bidInputLayout.error = "Please enter a valid amount"
                return@setOnClickListener
            }

            if (amountInSelectedCurrency < minBidInSelectedCurrency) {
                binding.bidInputLayout.error = "Bid must be at least ${currencyManager.formatPrice(minBidInSelectedCurrency, selectedCurrency)}"
                return@setOnClickListener
            }

            val amountInOriginalCurrency = currencyManager.convert(
                amountInSelectedCurrency,
                selectedCurrency,
                listingCurrency
            )

            placeBid(amountInOriginalCurrency)
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
