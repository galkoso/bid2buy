package com.example.bid2buy.ui.details

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bid2buy.R
import com.example.bid2buy.databinding.FragmentListingDetailsBinding
import com.example.bid2buy.model.Listing
import com.example.bid2buy.ui.bid.PlaceBidBottomSheetFragment
import com.example.bid2buy.util.CurrencyManager
import com.example.bid2buy.util.NetworkUtils
import com.google.android.material.tabs.TabLayoutMediator

class ListingDetailsFragment : Fragment() {

    private var _binding: FragmentListingDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ListingDetailsViewModel by viewModels()
    private val args: ListingDetailsFragmentArgs by navArgs()
    private lateinit var currencyManager: CurrencyManager

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
        
        currencyManager = CurrencyManager.getInstance(requireContext())
        setupHeader()
        observeViewModel()
        
        viewModel.loadListing(args.listingId)
        
        setupClickListeners()
        setupFragmentResultListeners()
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {
        binding.btnPlaceBid.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "No internet connection. Please connect to the internet to place a bid.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val listing = viewModel.listing.value ?: return@setOnClickListener
            val bottomSheet = PlaceBidBottomSheetFragment.newInstance(
                listingId = listing.id,
                currentHighestBid = listing.currentHighestBid ?: 0.0,
                startingPrice = listing.startingPrice,
                bidCount = listing.bidCount,
                listingCurrency = listing.currency
            )
            bottomSheet.show(parentFragmentManager, PlaceBidBottomSheetFragment.TAG)
        }

        binding.btnEdit.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "No internet connection. Please connect to the internet to edit your listing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val action = ListingDetailsFragmentDirections.actionListingDetailsFragmentToEditListingFragment(args.listingId)
            findNavController().navigate(action)
        }

        binding.btnDelete.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "No internet connection. Please connect to the internet to delete your listing.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.deleteListing()
            findNavController().navigateUp()
        }

        binding.btnViewBids.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "No internet connection. Please connect to the internet to view bid history.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val listingId = viewModel.listing.value?.id ?: return@setOnClickListener
            val bottomSheet = BidHistoryBottomSheetFragment.newInstance(listingId)
            bottomSheet.show(parentFragmentManager, BidHistoryBottomSheetFragment.TAG)
        }
    }

    private fun setupFragmentResultListeners() {
        parentFragmentManager.setFragmentResultListener("bid_placed", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("success")) {
                viewModel.loadListing(args.listingId)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.listing.observe(viewLifecycleOwner) { listing ->
            listing?.let { bindListingData(it) }
        }

        viewModel.isSignedIn.observe(viewLifecycleOwner) { isSignedIn ->
            binding.btnViewBids.isEnabled = isSignedIn
            binding.btnViewBids.alpha = if (isSignedIn) 1.0f else 0.5f
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
        } else {
            binding.tvStatus.text = "Active"
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
            binding.tvStatus.setTextColor(Color.WHITE)
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
        
        binding.tvSellerName.text = "by ${listing.createdByName}"
        
        val targetCurrency = currencyManager.getSelectedCurrency()
        val currentBid = listing.currentHighestBid
        if (currentBid != null) {
            val convertedBid = currencyManager.convert(currentBid, listing.currency, targetCurrency)
            binding.tvCurrentBid.text = currencyManager.formatPrice(convertedBid, targetCurrency)
            binding.tvBidLabel.text = "Current Highest Bid"
            binding.tvBidderName.text = "by ${listing.highestBidderName ?: "Unknown"}"
            binding.tvBidderName.visibility = View.VISIBLE
        } else {
            val convertedPrice = currencyManager.convert(listing.startingPrice, listing.currency, targetCurrency)
            binding.tvCurrentBid.text = currencyManager.formatPrice(convertedPrice, targetCurrency)
            binding.tvBidLabel.text = "Starting Price"
            binding.tvBidderName.visibility = View.GONE
        }
        
        val convertedStartingPrice = currencyManager.convert(listing.startingPrice, listing.currency, targetCurrency)
        binding.tvStartingPrice.text = "Starting price: ${currencyManager.formatPrice(convertedStartingPrice, targetCurrency)}"
        binding.btnViewBids.text = "View ${listing.bidCount} bids"

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
