package com.example.bid2buy.ui.myListings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bid2buy.R
import com.example.bid2buy.databinding.ItemMyListingBinding
import com.example.bid2buy.model.Listing
import com.example.bid2buy.util.CurrencyManager
import com.example.bid2buy.util.TimeUtils
import java.util.concurrent.TimeUnit

class MyListingsAdapter(private val onItemClick: (Listing) -> Unit) : ListAdapter<Listing, MyListingsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemMyListingBinding,
        private val onItemClick: (Listing) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(listing: Listing) {
            val context = binding.root.context
            binding.root.setOnClickListener { onItemClick(listing) }
            
            binding.tvTitle.text = listing.title
            binding.tvLocation.text = listing.location
            binding.tvCondition.text = listing.condition.lowercase()
            
            val currencyManager = CurrencyManager.getInstance(context)
            val targetCurrency = currencyManager.getSelectedCurrency()
            
            val originalPrice = if (listing.bidCount > 0) {
                listing.currentHighestBid ?: listing.startingPrice
            } else {
                listing.startingPrice
            }
            val originalCurrency = listing.currency

            val convertedPrice = currencyManager.convert(originalPrice, originalCurrency, targetCurrency)
            binding.tvPrice.text = currencyManager.formatPrice(convertedPrice, targetCurrency)
            
            binding.tvBidsCount.text = context.getString(R.string.bids_count_format, listing.bidCount)
            binding.ivGraph.visibility = if (listing.bidCount > 0) View.VISIBLE else View.GONE

            val now = TimeUtils.now()
            val closingAt = listing.closingAt
            
            if (closingAt != null) {
                val diff = closingAt.toDate().time - now.toDate().time
                if (diff > 0) {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    val minutes = (TimeUnit.MILLISECONDS.toMinutes(diff) % 60)
                    binding.tvTimeLeft.text = context.getString(R.string.time_left_format, hours, minutes)
                    binding.tvStatus.text = context.getString(R.string.status_active)
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                } else {
                    binding.tvTimeLeft.text = context.getString(R.string.time_left_closed)
                    binding.tvStatus.text = context.getString(R.string.status_closed)
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_closed)
                }
            } else {
                binding.tvTimeLeft.text = context.getString(R.string.time_left_na)
            }

            if (listing.photoUrls.isNotEmpty()) {
                Glide.with(binding.ivListingImage.context)
                    .load(listing.photoUrls[0])
                    .placeholder(R.drawable.shimmer_placeholder_rounded)
                    .centerCrop()
                    .into(binding.ivListingImage)
            } else {
                binding.ivListingImage.setImageResource(R.drawable.shimmer_placeholder_rounded)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Listing, newItem: Listing): Boolean = oldItem == newItem
    }
}
