package com.example.bid2buy.ui.myListings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bid2buy.R
import com.example.bid2buy.databinding.ItemMyListingBinding
import com.example.bid2buy.model.Listing
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

class MyListingsAdapter : ListAdapter<Listing, MyListingsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemMyListingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(listing: Listing) {
            binding.tvTitle.text = listing.title
            binding.tvLocation.text = listing.location
            binding.tvPrice.text = "₪${listing.startingPrice.toInt()}"
            binding.tvBidsCount.text = "2 bids"

            val now = Timestamp.now()
            val closingAt = listing.closingAt
            
            if (closingAt != null) {
                val diff = closingAt.toDate().time - now.toDate().time
                if (diff > 0) {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                    binding.tvTimeLeft.text = "${hours}h ${minutes}m"
                    binding.tvStatus.text = "Active"
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                } else {
                    binding.tvTimeLeft.text = "Closed"
                    binding.tvStatus.text = "Closed"
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_closed)
                }
            } else {
                binding.tvTimeLeft.text = "N/A"
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
