package com.example.bid2buy.ui.details

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckedTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bid2buy.R
import com.example.bid2buy.databinding.FragmentEditListingBinding
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.ListingsRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditListingFragment : Fragment() {

    private var _binding: FragmentEditListingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ListingDetailsViewModel by viewModels()
    private val args: EditListingFragmentArgs by navArgs()
    private val repository = ListingsRepository()

    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDropdowns()
        setupPriceControls()
        setupDateTimePickers()
        observeViewModel()
        
        viewModel.loadListing(args.listingId)

        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
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
    }

    private fun setupPriceControls() {
        binding.ivPriceUp.setOnClickListener {
            val currentPrice = binding.etPrice.text.toString().toIntOrNull() ?: 0
            binding.etPrice.setText((currentPrice + 1).toString())
        }

        binding.ivPriceDown.setOnClickListener {
            val currentPrice = binding.etPrice.text.toString().toIntOrNull() ?: 0
            if (currentPrice > 0) {
                binding.etPrice.setText((currentPrice - 1).toString())
            }
        }
    }

    private fun setupDateTimePickers() {
        binding.dateContainer.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDateTime.set(Calendar.YEAR, year)
                    selectedDateTime.set(Calendar.MONTH, month)
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateDisplay()
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        binding.timeContainer.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selectedDateTime.set(Calendar.MINUTE, minute)
                    updateTimeDisplay()
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        }
    }

    private fun observeViewModel() {
        viewModel.listing.observe(viewLifecycleOwner) { listing ->
            listing?.let { populateFields(it) }
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun populateFields(listing: Listing) {
        val hasBids = listing.bidCount > 0

        binding.cardWarning.visibility = if (hasBids) View.VISIBLE else View.GONE
        
        if (hasBids) {
            binding.tvEditableTitle.text = getString(R.string.editable_details_title)
            binding.tvOtherFieldsTitle.text = getString(R.string.locked_fields_title)
            binding.llPriceArrows.visibility = View.GONE
        } else {
            binding.tvEditableTitle.text = getString(R.string.item_details_title)
            binding.tvOtherFieldsTitle.text = getString(R.string.pricing_timeline_title)
            binding.llPriceArrows.visibility = View.VISIBLE
        }

        binding.etTitle.setText(listing.title)
        binding.etDescription.setText(listing.description)
        binding.etLocation.setText(listing.location)
        
        binding.autoCategory.setText(listing.category, false)
        binding.autoCondition.setText(listing.condition, false)
        binding.etPrice.setText(listing.startingPrice.toInt().toString())

        listing.closingAt?.let { timestamp ->
            selectedDateTime.time = timestamp.toDate()
            updateDateDisplay()
            updateTimeDisplay()
        }

        val isEditable = !hasBids
        binding.tilCategory.isEnabled = isEditable
        binding.tilCondition.isEnabled = isEditable
        binding.etPrice.isEnabled = isEditable
        binding.dateContainer.isEnabled = isEditable
        binding.timeContainer.isEnabled = isEditable
        
        val alpha = if (isEditable) 1.0f else 0.6f
        binding.tilCategory.alpha = alpha
        binding.tilCondition.alpha = alpha
        binding.etPrice.alpha = alpha
        binding.dateContainer.alpha = alpha
        binding.timeContainer.alpha = alpha
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = sdf.format(selectedDateTime.time)
    }

    private fun updateTimeDisplay() {
        val stf = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvSelectedTime.text = stf.format(selectedDateTime.time)
    }

    private fun saveChanges() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mutableMapOf<String, Any>(
            "title" to title,
            "description" to description,
            "location" to location
        )

        val listing = viewModel.listing.value ?: return
        if (listing.bidCount == 0) {
            val category = binding.autoCategory.text.toString()
            val condition = binding.autoCondition.text.toString()
            val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val closingAt = Timestamp(selectedDateTime.time)

            updates["category"] = category
            updates["condition"] = condition
            updates["startingPrice"] = price
            updates["closingAt"] = closingAt
        }

        lifecycleScope.launch {
            try {
                binding.btnSave.isEnabled = false
                binding.btnSave.text = "Saving..."
                repository.updateListing(listing.id, updates)
                Toast.makeText(requireContext(), "Changes saved successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = getString(R.string.save_changes)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
