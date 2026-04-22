package com.example.bid2buy.ui.details

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bid2buy.databinding.DialogBidHistoryBinding
import com.example.bid2buy.repositories.BidsRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BidHistoryBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: DialogBidHistoryBinding? = null
    private val binding get() = _binding!!

    private val repository = BidsRepository()
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

        val listingId = arguments?.getString(ARG_LISTING_ID) ?: return

        setupRecyclerView()
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
            repository.observeBidsForListing(listingId)
                .catch { e ->
                    Log.e(TAG, "Error observing bids", e)
                    Toast.makeText(context, "Failed to load bid history: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .collectLatest { bids ->
                    bidHistoryAdapter.submitList(bids)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
