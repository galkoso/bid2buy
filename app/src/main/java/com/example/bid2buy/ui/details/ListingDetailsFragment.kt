package com.example.bid2buy.ui.details

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bid2buy.R
import com.example.bid2buy.databinding.FragmentListingDetailsBinding
import com.example.bid2buy.model.Listing
import com.google.android.material.tabs.TabLayoutMediator

class ListingDetailsFragment : Fragment() {

    private var _binding: FragmentListingDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ListingDetailsViewModel by viewModels()
    private val args: ListingDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListingDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupHeader()
        observeViewModel()
        
        viewModel.loadListing(args.listingId)
        
        setupClickListeners()
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {
        binding.btnPlaceBid.setOnClickListener {
            Toast.makeText(context, "Bidding coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnEdit.setOnClickListener {
            Toast.makeText(context, "Edit coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.setOnClickListener {
            viewModel.deleteListing()
            findNavController().navigateUp()
        }

        binding.btnViewBids.setOnClickListener {
        }
    }

    private fun observeViewModel() {
        viewModel.listing.observe(viewLifecycleOwner) { listing ->
            listing?.let { bindListingData(it) }
        }

        viewModel.isOwner.observe(viewLifecycleOwner) { isOwner ->
            binding.llSellerActions.visibility = if (isOwner) View.VISIBLE else View.GONE
            binding.llBuyerActions.visibility = if (isOwner) View.GONE else View.VISIBLE
        }

        viewModel.canEdit.observe(viewLifecycleOwner) { canEdit ->
            binding.btnEdit.isEnabled = canEdit
            binding.btnEdit.alpha = if (canEdit) 1.0f else 0.5f
        }

        viewModel.canDelete.observe(viewLifecycleOwner) { canDelete ->
            binding.btnDelete.isEnabled = canDelete
            binding.btnDelete.alpha = if (canDelete) 1.0f else 0.5f
        }

        viewModel.canBid.observe(viewLifecycleOwner) { canBid ->
            binding.btnPlaceBid.isEnabled = canBid
            binding.btnPlaceBid.alpha = if (canBid) 1.0f else 0.5f
            updateBidButtonText()
        }

        viewModel.isClosed.observe(viewLifecycleOwner) { isClosed ->
            updateStatusBadge(isClosed)
            updateBidButtonText()
        }

        viewModel.timeRemaining.observe(viewLifecycleOwner) { timeText ->
            binding.tvTimeRemaining.text = timeText
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateStatusBadge(isClosed: Boolean) {
        if (isClosed) {
            binding.tvStatus.text = "Closed"
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_closed)
            binding.tvStatus.setTextColor(Color.WHITE)
            binding.tvStatus.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        } else {
            binding.tvStatus.text = "Active"
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
            binding.tvStatus.setTextColor(Color.WHITE)
            binding.tvStatus.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

    private fun updateBidButtonText() {
        val isClosed = viewModel.isClosed.value ?: false
        if (isClosed) {
            binding.btnPlaceBid.text = "Auction Closed"
        } else {
            binding.btnPlaceBid.text = "Place a Bid"
        }
    }

    private fun bindListingData(listing: Listing) {
        binding.tvTitle.text = listing.title
        binding.tvCategory.text = listing.category
        binding.tvCondition.text = listing.condition
        binding.tvLocation.text = listing.location
        binding.tvDescription.text = listing.description
        
        // Seller name
        binding.tvSellerName.text = "by ${listing.createdByName}"
        
        // Price and Bid Info
        val currentBid = listing.currentHighestBid
        if (currentBid != null) {
            binding.tvCurrentBid.text = "₪${currentBid.toInt()}"
            binding.tvBidLabel.text = "Current Highest Bid"
            binding.tvBidderName.text = "by ${listing.highestBidderName ?: "Unknown"}"
            binding.tvBidderName.visibility = View.VISIBLE
        } else {
            binding.tvCurrentBid.text = "₪${listing.startingPrice.toInt()}"
            binding.tvBidLabel.text = "Starting Price"
            binding.tvBidderName.visibility = View.GONE
        }
        
        binding.tvStartingPrice.text = "Starting price: ₪${listing.startingPrice.toInt()}"
        binding.btnViewBids.text = "View ${listing.bidCount} bids"

        // Image Gallery
        if (listing.photoUrls.isNotEmpty()) {
            val adapter = ImageGalleryAdapter(listing.photoUrls)
            binding.vpImageGallery.adapter = adapter
            
            if (listing.photoUrls.size > 1) {
                binding.tabIndicator.visibility = View.VISIBLE
                TabLayoutMediator(binding.tabIndicator, binding.vpImageGallery) { _, _ -> }.attach()
            } else {
                binding.tabIndicator.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
