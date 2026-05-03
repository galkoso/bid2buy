package com.example.bid2buy.ui.myListings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bid2buy.R
import com.example.bid2buy.databinding.FragmentMyListingsBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp

class MyListingsFragment : Fragment() {

    private var _binding: FragmentMyListingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MyListingsAdapter
    private val viewModel: MyListingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyListingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startListening()
        viewModel.startAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopAutoRefresh()
    }

    private fun setupRecyclerView() {
        adapter = MyListingsAdapter { listing ->
            val action = MyListingsFragmentDirections.actionNavigationListingsToListingDetailsFragment(listing.id)
            findNavController().navigate(action)
        }
        binding.rvListings.layoutManager = LinearLayoutManager(context)
        binding.rvListings.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.activeCount.observe(viewLifecycleOwner) { count ->
            binding.tabLayout.getTabAt(0)?.text = getString(R.string.active_with_count, count)
        }

        viewModel.closedCount.observe(viewLifecycleOwner) { count ->
            binding.tabLayout.getTabAt(1)?.text = getString(R.string.closed_with_count, count)
        }

        viewModel.listings.observe(viewLifecycleOwner) {
            updateList()
        }

        viewModel.timerPulse.observe(viewLifecycleOwner) {
            updateList()
        }
    }

    private fun setupListeners() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateList() {
        val allListings = viewModel.listings.value ?: emptyList()
        val now = Timestamp.now()
        
        val filteredListings = if (binding.tabLayout.selectedTabPosition == 0) {
            allListings.filter { it.closingAt != null && it.closingAt.toDate().time > now.toDate().time }
                .sortedBy { it.closingAt }
        } else {
            allListings.filter { it.closingAt == null || it.closingAt.toDate().time <= now.toDate().time }
                .sortedByDescending { it.closingAt }
        }
        
        adapter.submitList(filteredListings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
