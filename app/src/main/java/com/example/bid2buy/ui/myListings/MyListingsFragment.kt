package com.example.bid2buy.ui.myListings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bid2buy.databinding.FragmentMyListingsBinding
import com.google.firebase.Timestamp

class MyListingsFragment : Fragment() {

    private var _binding: FragmentMyListingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MyListingsAdapter
    private lateinit var viewModel: MyListingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(MyListingsViewModel::class.java)
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
        viewModel.stopListening()
        viewModel.stopAutoRefresh()
    }

    private fun setupRecyclerView() {
        adapter = MyListingsAdapter()
        binding.rvListings.layoutManager = LinearLayoutManager(context)
        binding.rvListings.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.activeCount.observe(viewLifecycleOwner) { count ->
            binding.btnActive.text = "Active ($count)"
        }

        viewModel.closedCount.observe(viewLifecycleOwner) { count ->
            binding.btnClosed.text = "Closed ($count)"
        }

        viewModel.listings.observe(viewLifecycleOwner) { listings ->
            updateList()
        }

        viewModel.timerPulse.observe(viewLifecycleOwner) {
            updateList()
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupListeners() {
        binding.toggleGroup.addOnButtonCheckedListener { _, _, _ ->
            updateList()
        }
    }

    private fun updateList() {
        val allListings = viewModel.listings.value ?: emptyList()
        val now = Timestamp.now()
        
        val filteredListings = if (binding.btnActive.isChecked) {
            // Sort Active: Soonest to close first (Ascending)
            allListings.filter { it.closingAt != null && it.closingAt.toDate().time > now.toDate().time }
                .sortedBy { it.closingAt }
        } else {
            // Sort Closed: Most recently closed first (Descending)
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
