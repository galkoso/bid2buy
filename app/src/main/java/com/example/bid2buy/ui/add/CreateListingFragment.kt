package com.example.bid2buy.ui.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckedTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bid2buy.R
import com.example.bid2buy.databinding.FragmentCreateListingBinding
import com.example.bid2buy.databinding.ItemPhotoPreviewBinding
import com.example.bid2buy.util.CurrencyManager
import com.example.bid2buy.util.NetworkUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateListingFragment : Fragment() {
    private var _binding: FragmentCreateListingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateListingViewModel by viewModels()
    private lateinit var currencyManager: CurrencyManager

    private val selectedImageUris = mutableListOf<Uri>()
    
    private var selectedDateTime: Calendar = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (selectedImageUris.size < 10) {
                selectedImageUris.add(it)
                renderPhotos()
            } else {
                Toast.makeText(requireContext(), getString(R.string.max_photos_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderPhotos() {
        binding.photoPreviewsContainer.removeAllViews()
        selectedImageUris.forEachIndexed { index, uri ->
            val previewBinding = ItemPhotoPreviewBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.photoPreviewsContainer,
                false
            )
            
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(previewBinding.ivPhoto)
            
            previewBinding.tvCoverBadge.visibility = if (index == 0) View.VISIBLE else View.GONE
            
            previewBinding.btnDelete.setOnClickListener {
                selectedImageUris.remove(uri)
                renderPhotos()
            }
            
            binding.photoPreviewsContainer.addView(previewBinding.root)
        }
        updatePhotoCount()
    }

    private fun updatePhotoCount() {
        binding.tvPhotoCount.text = getString(R.string.photo_count_format, selectedImageUris.size)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currencyManager = CurrencyManager.getInstance(requireContext())
        setupNavigation()
        setupInputs()
        setupDropdowns()
        setupPriceControls()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CreateListingState.Loading -> {
                            binding.btnPublish.isEnabled = false
                            binding.btnPublish.text = getString(R.string.publishing)
                        }
                        is CreateListingState.Success -> {
                            binding.btnPublish.isEnabled = true
                            binding.btnPublish.text = getString(R.string.publish_listing)
                            Toast.makeText(requireContext(), getString(R.string.listing_published_success), Toast.LENGTH_LONG).show()
                            findNavController().navigateUp()
                        }
                        is CreateListingState.Error -> {
                            binding.btnPublish.isEnabled = true
                            binding.btnPublish.text = getString(R.string.publish_listing)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        is CreateListingState.Idle -> {
                            binding.btnPublish.isEnabled = true
                            binding.btnPublish.text = getString(R.string.publish_listing)
                        }
                    }
                }
            }
        }
    }

    private fun setupNavigation() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPublish.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), getString(R.string.no_internet_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (validateInputs()) {
                val title = binding.etTitle.text.toString().trim()
                val description = binding.etDescription.text.toString().trim()
                val category = binding.autoCategory.text.toString()
                val condition = binding.autoCondition.text.toString()
                val location = binding.etLocation.text.toString().trim()
                val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val selectedCurrency = currencyManager.getSelectedCurrency()

                viewModel.publishListing(
                    title = title,
                    description = description,
                    category = category,
                    condition = condition,
                    location = location,
                    startingPrice = price,
                    currency = selectedCurrency,
                    closingDate = selectedDateTime.time,
                    imageUris = selectedImageUris
                )
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.etTitle.text.toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.title_required), Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.photo_required), Toast.LENGTH_SHORT).show()
            return false
        }

        val now = Calendar.getInstance()
        if (selectedDateTime.before(now)) {
            Toast.makeText(requireContext(), getString(R.string.future_time_error), Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (binding.autoCategory.text.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.category_required), Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.autoCondition.text.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.condition_required), Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.etLocation.text.toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.location_required), Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun setupDropdowns() {
        val categories = arrayOf(
            "Electronics", "Fashion", "Home & Garden", "Sports", "Accessories", "Other"
        )
        val categoryAdapter = SelectionAwareAdapter(requireContext(), R.layout.dropdown_item, categories, binding.autoCategory)
        binding.autoCategory.setAdapter(categoryAdapter)

        val conditions = arrayOf("New", "Like New", "Used", "Refurbished")
        val conditionAdapter = SelectionAwareAdapter(requireContext(), R.layout.dropdown_item, conditions, binding.autoCondition)
        binding.autoCondition.setAdapter(conditionAdapter)
        
        binding.autoCategory.dropDownVerticalOffset = 10
        binding.autoCondition.dropDownVerticalOffset = 10
    }

    private fun setupInputs() {
        binding.photoUploadContainer.setOnClickListener {
            if (selectedImageUris.size < 10) {
                getImage.launch("image/*")
            } else {
                Toast.makeText(requireContext(), getString(R.string.max_photos_error), Toast.LENGTH_SHORT).show()
            }
        }

        binding.dateContainer.setOnClickListener {
            val currentCalendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val tempCalendar = selectedDateTime.clone() as Calendar
                    tempCalendar.set(Calendar.YEAR, year)
                    tempCalendar.set(Calendar.MONTH, month)
                    tempCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    if (tempCalendar.before(currentCalendar)) {
                        Toast.makeText(requireContext(), getString(R.string.future_time_error), Toast.LENGTH_SHORT).show()
                        selectedDateTime = Calendar.getInstance().apply { 
                            add(Calendar.HOUR_OF_DAY, 1)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    } else {
                        selectedDateTime.set(Calendar.YEAR, year)
                        selectedDateTime.set(Calendar.MONTH, month)
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }
                    
                    updateDateDisplay()
                    updateTimeDisplay()
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = currentCalendar.timeInMillis
            datePickerDialog.show()
        }

        binding.timeContainer.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val tempCalendar = selectedDateTime.clone() as Calendar
                    tempCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    tempCalendar.set(Calendar.MINUTE, minute)
                    
                    if (tempCalendar.before(Calendar.getInstance())) {
                        Toast.makeText(requireContext(), getString(R.string.future_time_error), Toast.LENGTH_SHORT).show()
                    } else {
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, minute)
                        updateTimeDisplay()
                    }
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        }
    }

    private fun setupPriceControls() {
        val currentCurrency = currencyManager.getSelectedCurrency()
        binding.tvPriceLabel.text = getString(R.string.starting_price_label_format, currentCurrency)

        binding.ivPriceUp.setOnClickListener {
            val currentPrice = binding.etPrice.text.toString().toIntOrNull() ?: 0
            binding.etPrice.setText(getString(R.string.number_format, currentPrice + 1))
        }

        binding.ivPriceDown.setOnClickListener {
            val currentPrice = binding.etPrice.text.toString().toIntOrNull() ?: 0
            if (currentPrice > 0) {
                binding.etPrice.setText(getString(R.string.number_format, currentPrice - 1))
            }
        }
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = sdf.format(selectedDateTime.time)
        binding.tvSelectedDate.setTextColor(resources.getColor(android.R.color.black, null))
    }

    private fun updateTimeDisplay() {
        val stf = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvSelectedTime.text = stf.format(selectedDateTime.time)
        binding.tvSelectedTime.setTextColor(resources.getColor(android.R.color.black, null))
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
