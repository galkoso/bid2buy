package com.example.bid2buy.ui.bids

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bid2buy.R
import com.example.bid2buy.databinding.ItemBidCardBinding
import com.example.bid2buy.model.Listing
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

class BidsAdapter(private val onItemClick: (Listing) -> Unit) :
    ListAdapter<BidItemUiModel, BidsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBidCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemBidCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BidItemUiModel) {
            val listing = item.listing
            binding.textTitle.text = listing.title
            binding.textLocation.text = listing.location
            binding.textUserBid.text = "₪${item.userHighestBid.toInt()}"
            binding.textHighestBid.text = "₪${(listing.currentHighestBid ?: listing.startingPrice).toInt()}"
            binding.textBidCount.text = "${listing.bidCount} bids"
            
            // Time remaining logic
            val now = Timestamp.now()
            listing.closingAt?.let { closingAt ->
                val diff = closingAt.toDate().time - now.toDate().time
                if (diff > 0) {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    val minutes = (TimeUnit.MILLISECONDS.toMinutes(diff) % 60)
                    binding.textTime.text = "${hours}h ${minutes}m"
                    binding.textTime.visibility = View.VISIBLE
                } else {
                    binding.textTime.text = "Closed"
                    binding.textTime.visibility = View.VISIBLE
                }
            } ?: run {
                binding.textTime.visibility = View.GONE
            }

            // Set image
            if (listing.photoUrls.isNotEmpty()) {
                Glide.with(binding.itemImage.context)
                    .load(listing.photoUrls[0])
                    .into(binding.itemImage)
            }

            // Status handling
            when (item.status) {
                BidStatus.ACTIVE_WINNING -> {
                    setupStatusBadge(R.drawable.ic_graph_green, "Winning", "#E8F5E9", "#2E7D32")
                    binding.messageCard.visibility = View.GONE
                    binding.userBidStatusIcon.visibility = View.GONE
                }
                BidStatus.ACTIVE_OUTBID -> {
                    setupStatusBadge(R.drawable.ic_graph_red, "Outbid", "#F1F3F4", "#000000")
                    binding.userBidStatusIcon.visibility = View.VISIBLE
                    binding.messageCard.visibility = View.VISIBLE
                    binding.textStatusMessage.text = "You've been outbid!"
                    binding.messageCard.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF3E0")))
                    binding.textStatusMessage.setTextColor(Color.parseColor("#E65100"))
                }
                BidStatus.WON -> {
                    setupStatusBadge(R.drawable.ic_win, "Won", "#E8F5E9", "#2E7D32")
                    binding.userBidStatusIcon.visibility = View.GONE
                    binding.messageCard.visibility = View.VISIBLE
                    binding.textStatusMessage.text = "🎉 You won! Final: ₪${listing.currentHighestBid?.toInt()}"
                    binding.messageCard.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E8F5E9")))
                    binding.textStatusMessage.setTextColor(Color.parseColor("#2E7D32"))
                }
                BidStatus.LOST -> {
                    setupStatusBadge(null, "Closed", "#F5F5F5", "#757575")
                    binding.userBidStatusIcon.visibility = View.GONE
                    binding.messageCard.visibility = View.GONE
                }
            }

            binding.root.setOnClickListener { onItemClick(listing) }
        }

        private fun setupStatusBadge(iconRes: Int?, text: String, bgColor: String, textColor: String) {
            binding.statusText.text = text
            val color = Color.parseColor(textColor)
            binding.statusText.setTextColor(color)
            binding.statusBadgeLayout.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor(bgColor)))
            
            if (iconRes != null) {
                val drawable = ContextCompat.getDrawable(binding.statusText.context, iconRes)?.mutate()
                drawable?.let {
                    DrawableCompat.setTint(it, color)
                    binding.statusText.setCompoundDrawablesWithIntrinsicBounds(it, null, null, null)
                }
            } else {
                binding.statusText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BidItemUiModel>() {
        override fun areItemsTheSame(oldItem: BidItemUiModel, newItem: BidItemUiModel): Boolean {
            return oldItem.listing.id == newItem.listing.id
        }

        override fun areContentsTheSame(oldItem: BidItemUiModel, newItem: BidItemUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
