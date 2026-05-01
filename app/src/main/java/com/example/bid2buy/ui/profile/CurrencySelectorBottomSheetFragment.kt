package com.example.bid2buy.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bid2buy.R
import com.example.bid2buy.databinding.BottomSheetCurrencySelectorBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CurrencyModel(
    val code: String,
    val name: String,
    val symbol: String,
    val isSelected: Boolean = false
)

class CurrencySelectorBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetCurrencySelectorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCurrencySelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.currencyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCurrency.collectLatest { selectedCode ->
                    val currencies = getCurrencies(selectedCode)
                    binding.currencyRecyclerView.adapter = CurrencyAdapter(currencies) { selectedItem ->
                        viewModel.selectCurrency(selectedItem.code)
                        dismiss()
                    }
                }
            }
        }

        binding.closeBtn.setOnClickListener {
            dismiss()
        }
    }

    private fun getCurrencies(selectedCode: String): List<CurrencyModel> {
        return listOf(
            CurrencyModel("ILS", "Israeli Shekel", "₪", selectedCode == "ILS"),
            CurrencyModel("USD", "US Dollar", "$", selectedCode == "USD"),
            CurrencyModel("EUR", "Euro", "€", selectedCode == "EUR"),
            CurrencyModel("GBP", "British Pound", "£", selectedCode == "GBP"),
            CurrencyModel("JPY", "Japanese Yen", "¥", selectedCode == "JPY"),
            CurrencyModel("AUD", "Australian Dollar", "A$", selectedCode == "AUD"),
            CurrencyModel("CAD", "Canadian Dollar", "C$", selectedCode == "CAD"),
            CurrencyModel("CHF", "Swiss Franc", "CHF", selectedCode == "CHF")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CurrencyAdapter(
        private val items: List<CurrencyModel>,
        private val onItemClick: (CurrencyModel) -> Unit
    ) : RecyclerView.Adapter<CurrencyAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val symbol: TextView = view.findViewById(R.id.currencySymbol)
            val name: TextView = view.findViewById(R.id.currencyName)
            val code: TextView = view.findViewById(R.id.currencyCode)
            val check: ImageView = view.findViewById(R.id.checkIcon)

            fun bind(item: CurrencyModel) {
                symbol.text = item.symbol
                name.text = item.name
                code.text = item.code
                check.visibility = if (item.isSelected) View.VISIBLE else View.GONE
                
                itemView.setOnClickListener { onItemClick(item) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_currency, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size
    }
}
