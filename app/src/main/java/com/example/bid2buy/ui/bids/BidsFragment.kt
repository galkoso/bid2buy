package com.example.bid2buy.ui.bids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bid2buy.databinding.FragmentBidsBinding
import com.google.android.material.tabs.TabLayout

class BidsFragment : Fragment() {

    private var _binding: FragmentBidsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BidsViewModel by activityViewModels()
    private val args: BidsFragmentArgs by navArgs()
    private lateinit var adapter: BidsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBidsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        
        val initialTab = args.initialTab
        if (initialTab in 0..2) {
            binding.tabLayout.getTabAt(initialTab)?.select()
        }
        
        setupTabs()
        setupObservers()
        setupSwipeRefresh()

        updateListForTab(initialTab)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadBids(forceRefresh = false)
    }

    private fun setupRecyclerView() {
        adapter = BidsAdapter { listing ->
            val action = BidsFragmentDirections.actionNavigationBidsToListingDetailsFragment(listing.id)
            findNavController().navigate(action)
        }
        binding.recyclerViewBids.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewBids.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateListForTab(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateListForTab(position: Int) {
        val list = when (position) {
            0 -> viewModel.activeBids.value
            1 -> viewModel.wonBids.value
            2 -> viewModel.lostBids.value
            else -> emptyList()
        } ?: emptyList()
        
        adapter.submitList(list)
        
        if (list.isEmpty()) {
            if (viewModel.isLoading.value != true) {
                binding.emptyState.visibility = View.VISIBLE
                binding.emptyState.text = when (position) {
                    0 -> "You have no active bids"
                    1 -> "You haven't won any auctions yet"
                    2 -> "No lost auctions found"
                    else -> ""
                }
            } else {
                binding.emptyState.visibility = View.GONE
            }
        } else {
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        viewModel.activeBids.observe(viewLifecycleOwner) {
            if (binding.tabLayout.selectedTabPosition == 0) {
                updateListForTab(0)
            }
            updateTabBadge(0, it.size)
        }
        viewModel.wonBids.observe(viewLifecycleOwner) {
            if (binding.tabLayout.selectedTabPosition == 1) {
                updateListForTab(1)
            }
            updateTabBadge(1, it.size)
        }
        viewModel.lostBids.observe(viewLifecycleOwner) {
            if (binding.tabLayout.selectedTabPosition == 2) {
                updateListForTab(2)
            }
            updateTabBadge(2, it.size)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                if (!binding.swipeRefresh.isRefreshing && adapter.itemCount == 0) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                }
            } else {
                binding.swipeRefresh.isRefreshing = false
                binding.progressBar.visibility = View.GONE
                updateListForTab(binding.tabLayout.selectedTabPosition)
            }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.timerPulse.observe(viewLifecycleOwner) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateTabBadge(position: Int, count: Int) {
        val tab = binding.tabLayout.getTabAt(position)
        val title = when (position) {
            0 -> "Active"
            1 -> "Won"
            2 -> "Lost"
            else -> ""
        }
        tab?.text = "$title ($count)"
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadBids(forceRefresh = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
