package com.example.bid2buy.ui.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bid2buy.R
import com.example.bid2buy.databinding.ItemImageGalleryBinding

class ImageGalleryAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<ImageGalleryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemImageGalleryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageGalleryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.binding.ivGalleryImage.context)
            .load(imageUrls[position])
            .placeholder(R.drawable.shimmer_placeholder_rounded)
            .fitCenter()
            .into(holder.binding.ivGalleryImage)
    }

    override fun getItemCount(): Int = imageUrls.size
}
