package com.example.bid2buy.ui.home

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckedTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bid2buy.R
import com.example.bid2buy.databinding.DialogFilterBinding
import com.example.bid2buy.databinding.FragmentHomeBinding
import com.example.bid2buy.ui.myListings.MyListingsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog

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
        setupFilterButton()
        setupSearchButton()
        observeViewModel()

        homeViewModel.startListening()

        return root
    }

    private fun setupRecyclerView() {
        adapter = MyListingsAdapter { listing ->
            val action = HomeFragmentDirections.actionNavigationHomeToListingDetailsFragment(listing.id)
            findNavController().navigate(action)
        }
        binding.rvBrowse.layoutManager = LinearLayoutManager(context)
        binding.rvBrowse.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            homeViewModel.refresh()
        }
    }

    private fun setupFilterButton() {
        binding.ivFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupSearchButton() {
        binding.ivSearch.setOnClickListener {
            if (binding.llSearchContainer.visibility == View.VISIBLE) {
                binding.llSearchContainer.visibility = View.GONE
                binding.etSearch.text?.clear()
                homeViewModel.setSearchQuery(null)
            } else {
                binding.llSearchContainer.visibility = View.VISIBLE
                binding.etSearch.requestFocus()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                homeViewModel.setSearchQuery(s?.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showFilterDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogFilterBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val (savedCategory, savedCondition, savedPrice) = homeViewModel.getCurrentFilters()

        setupDialogDropdowns(dialogBinding, savedCategory, savedCondition, savedPrice)

        dialogBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnApply.setOnClickListener {
            val category = dialogBinding.autoCategoryFilter.text.toString()
            val condition = dialogBinding.autoConditionFilter.text.toString()
            val priceRange = dialogBinding.autoPriceFilter.text.toString()
            
            homeViewModel.setFilters(category, condition, priceRange)
            dialog.dismiss()
        }

        dialogBinding.btnClear.setOnClickListener {
            homeViewModel.clearFilters()
            
            dialogBinding.autoCategoryFilter.setText(getString(R.string.all_categories), false)
            dialogBinding.autoConditionFilter.setText(getString(R.string.all_conditions), false)
            dialogBinding.autoPriceFilter.setText(getString(R.string.all_prices), false)
            
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupDialogDropdowns(
        dialogBinding: DialogFilterBinding,
        savedCategory: String?,
        savedCondition: String?,
        savedPrice: String?
    ) {
        val categories = arrayOf(
            getString(R.string.all_categories), "Electronics", "Fashion", "Home & Garden", "Sports", "Accessories", "Other"
        )
        setupDropdown(dialogBinding.autoCategoryFilter, categories, savedCategory ?: getString(R.string.all_categories))

        val conditions = arrayOf(
            getString(R.string.all_conditions), "New", "Like New", "Used", "Refurbished"
        )
        setupDropdown(dialogBinding.autoConditionFilter, conditions, savedCondition ?: getString(R.string.all_conditions))

        val priceRanges = arrayOf(
            getString(R.string.all_prices),
            getString(R.string.under_100),
            getString(R.string.price_100_500),
            getString(R.string.over_500)
        )
        setupDropdown(dialogBinding.autoPriceFilter, priceRanges, savedPrice ?: getString(R.string.all_prices))
    }

    private fun setupDropdown(view: AutoCompleteTextView, options: Array<String>, selectedValue: String) {
        val adapter = SelectionAwareAdapter(requireContext(), R.layout.dropdown_item, options, view)
        view.setAdapter(adapter)
        view.setText(selectedValue, false)
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

    private class SelectionAwareAdapter(
        context: Context,
        resource: Int,
        objects: Array<String>,
        private val autoCompleteTextView: AutoCompleteTextView
    ) : ArrayAdapter<String>(context, resource, objects) {
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val itemText = getItem(position)
            val currentText = autoCompleteTextView.text.toString()
            val isSelected = itemText != null && itemText == currentText

            if (view is CheckedTextView) {
                view.isChecked = isSelected
                view.isActivated = isSelected
                view.isSelected = isSelected
                view.refreshDrawableState()
            }
            return view
        }
    }
}
