package com.example.bid2buy.ui.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bid2buy.databinding.ItemBidHistoryBinding
import com.example.bid2buy.model.Bid
import java.text.SimpleDateFormat
import java.util.*

class BidHistoryAdapter : ListAdapter<BidHistoryAdapter.BidHistoryItem, BidHistoryAdapter.ViewHolder>(DiffCallback()) {

    data class BidHistoryItem(
        val bid: Bid,
        val isHighest: Boolean
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBidHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemBidHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("M/d/yyyy, h:mm:ss a", Locale.getDefault())

        fun bind(item: BidHistoryItem) {
            val bid = item.bid
            binding.tvBidderName.text = bid.bidderName
            binding.tvAmount.text = "₪${bid.amount.toInt()}"
            binding.tvTimestamp.text = bid.timestamp?.toDate()?.let { dateFormat.format(it) } ?: ""
            
            // Explicitly set visibility to handle view reuse
            binding.tvHighestLabel.visibility = if (item.isHighest) View.VISIBLE else View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BidHistoryItem>() {
        override fun areItemsTheSame(oldItem: BidHistoryItem, newItem: BidHistoryItem) = 
            oldItem.bid.id == newItem.bid.id
            
        override fun areContentsTheSame(oldItem: BidHistoryItem, newItem: BidHistoryItem) = 
            oldItem == newItem
    }
}
