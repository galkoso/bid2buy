package com.example.bid2buy.ui.details

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view. View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.databinding.DialogBidHistoryBinding
import com.example.bid2buy.repositories.BidsRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BidHistoryBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: DialogBidHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BidsRepository
    private lateinit var bidHistoryAdapter: BidHistoryAdapter

    companion object {
        private const val ARG_LISTING_ID = "listing_id"
        const val TAG = "BidHistoryBottomSheet"

        fun newInstance(listingId: String): BidHistoryBottomSheetFragment {
            return BidHistoryBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LISTING_ID, listingId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBidHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext().applicationContext)
        repository = BidsRepository(database.bidDao(), database.listingDao())

        val listingId = arguments?.getString(ARG_LISTING_ID) ?: return

        setupRecyclerView()
        
        // Refresh bids from network to sync to Room (Rule: Local and Remote storage)
        lifecycleScope.launch {
            repository.refreshBidsForListing(listingId)
        }

        observeBids(listingId)

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        bidHistoryAdapter = BidHistoryAdapter()
        binding.rvBids.apply {
            adapter = bidHistoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeBids(listingId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Single Source of Truth: Observe Room
            repository.observeBidsForListing(listingId)
                .catch { e ->
                    Log.e(TAG, "Error observing bids", e)
                    Toast.makeText(context, "Failed to load bid history: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .collectLatest { bids ->
                    val uiItems = bids.mapIndexed { index, bid ->
                        BidHistoryAdapter.BidHistoryItem(
                            bid = bid,
                            isHighest = index == 0
                        )
                    }
                    
                    bidHistoryAdapter.submitList(uiItems) {
                        if (isAdded) {
                            binding.rvBids.scrollToPosition(0)
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
