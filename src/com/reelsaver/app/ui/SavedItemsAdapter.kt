package com.reelsaver.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.reelsaver.app.R
import com.reelsaver.app.databinding.ItemSavedBinding
import com.reelsaver.app.model.SavedItem
import com.reelsaver.app.model.SavedItemType
import java.text.SimpleDateFormat
import java.util.Locale

class SavedItemsAdapter(
    private val onItemClick: (SavedItem) -> Unit,
    private val onDeleteClick: (SavedItem) -> Unit
) : ListAdapter<SavedItem, SavedItemsAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SavedItem>() {
            override fun areItemsTheSame(a: SavedItem, b: SavedItem) = a.id == b.id
            override fun areContentsTheSame(a: SavedItem, b: SavedItem) = a == b
        }
        val DATE_FORMAT = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSavedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SavedItem) {
            binding.apply {
                // Type badge
                when (item.type) {
                    SavedItemType.YOUTUBE_VIDEO -> {
                        tvTypeBadge.text = "▶ YouTube"
                        tvTypeBadge.setBackgroundResource(R.drawable.bg_tag_youtube)
                        tvTitle.text = if (item.title == "YouTube Video")
                            "YouTube Video" else item.title
                        ivThumbnail.visibility = View.VISIBLE
                        Glide.with(root.context)
                            .load("https://img.youtube.com/vi/${item.youtubeVideoId}/mqdefault.jpg")
                            .placeholder(R.drawable.ic_video_placeholder)
                            .into(ivThumbnail)
                    }
                    SavedItemType.PLACE -> {
                        tvTypeBadge.text = "📍 Place"
                        tvTypeBadge.setBackgroundResource(R.drawable.bg_tag_place)
                        tvTitle.text = item.placeName.ifEmpty { item.title }
                        ivThumbnail.visibility = View.GONE
                    }
                    SavedItemType.INSTAGRAM_REEL -> {
                        tvTypeBadge.text = "🎥 Reel"
                        tvTypeBadge.setBackgroundResource(R.drawable.bg_tag_instagram)
                        tvTitle.text = "Instagram Reel"
                        ivThumbnail.visibility = View.GONE
                    }
                    else -> {
                        tvTypeBadge.text = "🔗 Link"
                        tvTypeBadge.setBackgroundResource(R.drawable.bg_tag_default)
                        tvTitle.text = item.title
                        ivThumbnail.visibility = View.GONE
                    }
                }

                // Description / tags
                val desc = buildString {
                    if (item.description.isNotEmpty()) append(item.description.take(80))
                    if (item.tags.isNotEmpty()) {
                        if (isNotEmpty()) append("\n")
                        append(item.tags.split(",").take(3).joinToString(" ") { "#$it" })
                    }
                }
                tvDescription.text = desc
                tvDescription.visibility = if (desc.isEmpty()) View.GONE else View.VISIBLE

                // Notes
                tvNotes.text = item.notes
                tvNotes.visibility = if (item.notes.isEmpty()) View.GONE else View.VISIBLE

                // Date
                tvDate.text = DATE_FORMAT.format(item.savedAt)

                // Clicks
                root.setOnClickListener { onItemClick(item) }
                btnDelete.setOnClickListener { onDeleteClick(item) }
            }
        }
    }
}
