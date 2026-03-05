package com.example.bid2buy.ui.add

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
import androidx.navigation.fragment.findNavController
import com.example.bid2buy.R
import com.example.bid2buy.databinding.FragmentCreateListingBinding
import java.text.SimpleDateFormat
import java.util.*

class CreateListingFragment : Fragment() {
    private var _binding: FragmentCreateListingBinding? = null
    private val binding get() = _binding!!
    
    private var selectedDateTime: Calendar = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
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

        setupNavigation()
        setupInputs()
        setupDropdowns()
        setupPriceControls()
    }

    private fun setupNavigation() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPublish.setOnClickListener {
            if (validateInputs()) {
                Toast.makeText(requireContext(), "Listing created (Simulation)", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (selectedDateTime.before(Calendar.getInstance())) {
            Toast.makeText(requireContext(), "Please select a future date and time", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (binding.autoCategory.text.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.autoCondition.text.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a condition", Toast.LENGTH_SHORT).show()
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
        
        binding.autoCategory.setDropDownVerticalOffset(10)
        binding.autoCondition.setDropDownVerticalOffset(10)
    }

    private fun setupInputs() {
        binding.photoUploadContainer.setOnClickListener {
            Toast.makeText(requireContext(), "Photo picker would open here", Toast.LENGTH_SHORT).show()
        }

        binding.dateContainer.setOnClickListener {
            val currentCalendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDateTime.set(Calendar.YEAR, year)
                    selectedDateTime.set(Calendar.MONTH, month)
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.tvSelectedDate.text = sdf.format(selectedDateTime.time)
                    binding.tvSelectedDate.setTextColor(resources.getColor(android.R.color.black, null))
                    
                    if (selectedDateTime.before(Calendar.getInstance())) {
                         selectedDateTime = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                         updateTimeDisplay()
                    }
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
                        Toast.makeText(requireContext(), "Please select a future time", Toast.LENGTH_SHORT).show()
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
                // Ensure the view is redrawn with the correct states
                view.refreshDrawableState()
            }
            return view
        }
    }
}
