package com.example.bid2buy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bid2buy.databinding.FragmentHomeBinding
import com.example.bid2buy.ui.myListings.MyListingsAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MyListingsAdapter
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        homeViewModel.startListening()

        return root
    }

    private fun setupRecyclerView() {
        adapter = MyListingsAdapter()
        binding.rvBrowse.layoutManager = LinearLayoutManager(context)
        binding.rvBrowse.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            homeViewModel.refresh()
        }
    }

    private fun observeViewModel() {
        homeViewModel.listings.observe(viewLifecycleOwner) { listings ->
            adapter.submitList(listings) {
                adapter.notifyDataSetChanged()
            }
            binding.tvItemCount.text = "${listings.size} items"
            binding.swipeRefresh.isRefreshing = false
        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}