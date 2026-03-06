package com.reelsaver.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.reelsaver.app.R
import com.reelsaver.app.ReelSaverApp
import com.reelsaver.app.databinding.ActivityMainBinding
import com.reelsaver.app.model.SavedItem
import com.reelsaver.app.model.SavedItemType

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SavedItemsAdapter
    private var allItems = listOf<SavedItem>()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        setupFilterChips()
        loadItems()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun setupRecyclerView() {
        adapter = SavedItemsAdapter(
            onItemClick = { item -> openItem(item) },
            onDeleteClick = { item -> deleteItem(item) }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterItems(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        val chips = mapOf(
            binding.chipAll to "All",
            binding.chipPlaces to "Places",
            binding.chipYoutube to "YouTube",
            binding.chipReels to "Reels"
        )

        chips.forEach { (chip, filter) ->
            chip.setOnClickListener {
                chips.keys.forEach { it.isChecked = false }
                chip.isChecked = true
                currentFilter = filter
                applyFilter()
            }
        }

        binding.chipAll.isChecked = true
    }

    private fun loadItems() {
        val app = application as ReelSaverApp
        allItems = app.database.getAllItems()
        applyFilter()
        updateEmptyState()
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "Places" -> allItems.filter { it.type == SavedItemType.PLACE }
            "YouTube" -> allItems.filter { it.type == SavedItemType.YOUTUBE_VIDEO }
            "Reels" -> allItems.filter { it.type == SavedItemType.INSTAGRAM_REEL }
            else -> allItems
        }

        val search = binding.etSearch.text.toString()
        filterItems(search, filtered)
    }

    private fun filterItems(query: String, source: List<SavedItem> = allItems) {
        val filtered = if (query.isEmpty()) source
        else source.filter { item ->
            item.title.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true) ||
            item.placeName.contains(query, ignoreCase = true) ||
            item.tags.contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean = allItems.isEmpty()) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun openItem(item: SavedItem) {
        when (item.type) {
            SavedItemType.YOUTUBE_VIDEO -> {
                val url = "https://www.youtube.com/watch?v=${item.youtubeVideoId}"
                    .takeIf { item.youtubeVideoId.isNotEmpty() } ?: item.url
                openUrl(url)
            }
            SavedItemType.PLACE -> {
                if (item.url.isNotEmpty()) {
                    openUrl(item.url)
                } else if (item.placeName.isNotEmpty()) {
                    val mapsUrl = "https://maps.google.com/?q=${Uri.encode(item.placeName)}"
                    openUrl(mapsUrl)
                }
            }
            else -> {
                if (item.url.isNotEmpty()) openUrl(item.url)
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun deleteItem(item: SavedItem) {
        val app = application as ReelSaverApp
        if (app.database.deleteItem(item.id)) {
            loadItems()
        }
    }
}
