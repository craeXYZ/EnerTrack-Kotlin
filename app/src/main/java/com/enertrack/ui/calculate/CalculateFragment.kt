package com.enertrack.ui.calculate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.enertrack.R
import com.enertrack.data.model.DeviceResponse
import com.enertrack.databinding.FragmentCalculateBinding
import com.enertrack.databinding.BottomSheetAddDeviceBinding
import com.enertrack.databinding.ItemCalculateDeviceBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.util.Locale
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.enertrack.data.repository.Result

class CalculateFragment : Fragment() {

    private var _binding: FragmentCalculateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalculateViewModel by viewModels {
        CalculateViewModelFactory(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalculateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupTabs()

        viewModel.fetchBrands()
        viewModel.fetchHouseCapacities()

        observeViewModel()
    }

    private fun setupClickListeners() {
        // --- MANUAL CALCULATOR ---
        binding.fabAddDevice.setOnClickListener {
            showAddDeviceBottomSheet()
        }

        binding.btnSaveHistory.setOnClickListener {
            val capacity = binding.dropdownCapacity.text.toString()

            if (capacity.isBlank()) {
                Toast.makeText(requireContext(), "Please select house capacity first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (viewModel.applianceList.value.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No devices to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.submitDeviceList(capacity, "Postpaid")
            Toast.makeText(requireContext(), "Saving...", Toast.LENGTH_SHORT).show()
        }

        // --- IOT NAVIGATION & REFRESH ---
        binding.itemIotDevice1.setOnClickListener {
            binding.layoutIotList.visibility = View.GONE
            binding.layoutIotDetail.visibility = View.VISIBLE
            binding.tvDetailDeviceName.text = "Main Smart Meter"
            binding.tvDetailDeviceInfo.text = "Smart Meter V2.0"
            binding.tvLastUpdate.text = "Last update: Just now"
            viewModel.refreshIoTConnection()
        }

        binding.itemIotDevice2.setOnClickListener {
            binding.layoutIotList.visibility = View.GONE
            binding.layoutIotDetail.visibility = View.VISIBLE
            binding.tvDetailDeviceName.text = "Smart Plug Fridge"
            binding.tvDetailDeviceInfo.text = "Kitchen • Offline"
            binding.tvLastUpdate.text = "Last update: 2 hours ago"
            binding.tvMainWatt.text = "0 W"
            binding.tvDetailVoltage.text = "0 V"
            binding.tvDetailCurrent.text = "0 A"
            binding.tvStatusLabel.text = "Status: Offline"
        }

        binding.btnBackToList.setOnClickListener {
            binding.layoutIotDetail.visibility = View.GONE
            binding.layoutIotList.visibility = View.VISIBLE
        }

        binding.btnRefreshIot.setOnClickListener {
            viewModel.refreshIoTConnection()
            Toast.makeText(requireContext(), "Refreshing connection...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddDeviceBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetAddDeviceBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        var selectedDeviceData: DeviceResponse? = null

        viewModel.brandOptions.observe(viewLifecycleOwner) { brands ->
            if (!brands.isNullOrEmpty()) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brands)
                sheetBinding.dropdownBrand.setAdapter(adapter)
                sheetBinding.dropdownBrand.setOnClickListener { sheetBinding.dropdownBrand.showDropDown() }
            }
        }

        sheetBinding.dropdownBrand.setOnItemClickListener { parent, _, position, _ ->
            val selectedBrand = parent.getItemAtPosition(position).toString()
            sheetBinding.dropdownDevice.text = null
            sheetBinding.etPower.text = null
            sheetBinding.layoutDeviceName.isEnabled = false
            selectedDeviceData = null
            viewModel.fetchDevicesByBrand(selectedBrand)
        }

        viewModel.deviceOptionsForBrand.observe(viewLifecycleOwner) { devices ->
            if (!devices.isNullOrEmpty()) {
                val deviceNames = devices.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, deviceNames)
                sheetBinding.dropdownDevice.setAdapter(adapter)
                sheetBinding.layoutDeviceName.isEnabled = true
                sheetBinding.dropdownDevice.setOnClickListener { sheetBinding.dropdownDevice.showDropDown() }
            }
        }

        sheetBinding.dropdownDevice.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            val originalList = viewModel.deviceOptionsForBrand.value
            selectedDeviceData = originalList?.find { it.name == selectedName }
            if (selectedDeviceData != null) {
                sheetBinding.etPower.setText(selectedDeviceData?.powerWatt.toString())
            }
        }

        sheetBinding.btnSaveDevice.setOnClickListener {
            val brand = sheetBinding.dropdownBrand.text.toString()
            val deviceName = sheetBinding.dropdownDevice.text.toString()
            val powerStr = sheetBinding.etPower.text.toString()
            val usageStr = sheetBinding.etUsage.text.toString()

            if (deviceName.isBlank() || powerStr.isBlank() || usageStr.isBlank()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val catId = selectedDeviceData?.categoryId
                val currentList = viewModel.applianceList.value
                currentList?.forEach { viewModel.deleteAppliance(it.id) }
                viewModel.addOrUpdateAppliance(deviceName, brand, null, powerStr, usageStr, "1", catId)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun setupTabs() {
        binding.tabLayoutCalculate.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.calculatorContentContainer.visibility = View.VISIBLE
                        binding.iotContentContainer.visibility = View.GONE

                        val list = viewModel.applianceList.value
                        if (list.isNullOrEmpty()) {
                            binding.fabAddDevice.visibility = View.VISIBLE
                            binding.cardSummary.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.fabAddDevice.visibility = View.GONE
                            binding.cardSummary.visibility = View.VISIBLE
                            binding.layoutEmptyState.visibility = View.GONE
                        }
                    }
                    1 -> {
                        binding.calculatorContentContainer.visibility = View.GONE
                        binding.iotContentContainer.visibility = View.VISIBLE
                        binding.layoutIotList.visibility = View.VISIBLE
                        binding.layoutIotDetail.visibility = View.GONE
                        binding.fabAddDevice.visibility = View.GONE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.applianceList.observe(viewLifecycleOwner) { list ->
            if (binding.tabLayoutCalculate.selectedTabPosition == 0) {
                if (list.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.cardSummary.visibility = View.GONE
                    binding.fabAddDevice.visibility = View.VISIBLE
                    binding.tvTotalSummary.text = "Total Est: Rp 0 / month"
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.cardSummary.visibility = View.VISIBLE
                    binding.fabAddDevice.visibility = View.GONE

                    val item = list.last()
                    updateDeviceItemView(item)
                    updateResultCard(item)
                }
            }
        }

        viewModel.houseCapacityOptions.observe(viewLifecycleOwner) { capacities ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, capacities)
            binding.dropdownCapacity.setAdapter(adapter)
            binding.dropdownCapacity.setOnItemClickListener { _, _, position, _ ->
                viewModel.selectedHouseCapacity.value = capacities[position]
                val list = viewModel.applianceList.value
                if (!list.isNullOrEmpty()) updateResultCard(list.last())
            }
        }

        viewModel.submissionStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> Toast.makeText(requireContext(), "✅ Successfully saved!", Toast.LENGTH_LONG).show()
                is Result.Failure -> Toast.makeText(requireContext(), "❌ Failed: ${result.exception.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.iotStatus.observe(viewLifecycleOwner) { status ->
            binding.tvStatusLabel.text = "Status: $status"
            if (status == "Connected") {
                binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.energyGreen))
            } else {
                binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.energyOrange))
            }
        }

        viewModel.iotPower.observe(viewLifecycleOwner) { watt ->
            binding.tvMainWatt.text = "$watt W"
        }

        viewModel.iotVoltage.observe(viewLifecycleOwner) { volt ->
            binding.tvDetailVoltage.text = "%.1f V".format(volt)
        }

        viewModel.iotCurrent.observe(viewLifecycleOwner) { ampere ->
            binding.tvDetailCurrent.text = "%.2f A".format(ampere)
        }
    }

    private fun updateDeviceItemView(item: com.enertrack.data.model.Appliance) {
        binding.containerDeviceItem.removeAllViews()
        val itemBinding = ItemCalculateDeviceBinding.inflate(layoutInflater, binding.containerDeviceItem, true)
        itemBinding.tvDeviceName.text = item.name
        itemBinding.tvDeviceDetails.text = "${item.powerRating} Watt • ${item.dailyUsage} Hours/day"
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        currencyFormat.maximumFractionDigits = 0
        val costStr = currencyFormat.format(item.monthlyCost).replace("Rp", "Rp ")
        itemBinding.tvDeviceCost.text = "Est. $costStr"
        itemBinding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Device")
                .setMessage("Are you sure you want to remove this device?")
                .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                .setPositiveButton("Remove") { dialogInterface, _ ->
                    viewModel.deleteAppliance(item.id)
                    dialogInterface.dismiss()
                    Toast.makeText(requireContext(), "Data reset", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun updateResultCard(item: com.enertrack.data.model.Appliance) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        currencyFormat.maximumFractionDigits = 0
        val costStr = currencyFormat.format(item.monthlyCost).replace("Rp", "Rp ")
        binding.tvTotalMonthlyCost.text = costStr
        binding.tvTotalSummary.text = "Total Est: $costStr / month"
        binding.tvTotalDaily.text = "${"%.2f".format(item.dailyEnergy)} kWh"
        val capacityStr = viewModel.selectedHouseCapacity.value?.replace(" VA", "")?.replace(".", "") ?: "0"
        val capacity = capacityStr.toDoubleOrNull() ?: 0.0
        val totalPower = item.powerRating
        if (capacity > 0) {
            val percentage = (totalPower / capacity) * 100
            binding.tvWarningDetails.text = "Load ${totalPower.toInt()}W (${"%.1f".format(percentage)}% of capacity)"
            when {
                percentage > 90 -> {
                    binding.tvWarningMessage.text = "⚠️ Danger: High Load!"
                    setWarningColors(R.color.dangerRed, R.color.bgLightRed)
                }
                percentage > 50 -> {
                    binding.tvWarningMessage.text = "⚠️ Warning: High Consumption"
                    setWarningColors(R.color.energyOrange, R.color.bgLightOrange)
                }
                else -> {
                    binding.tvWarningMessage.text = "✅ Safe Usage"
                    setWarningColors(R.color.energyGreen, R.color.bgLightGreen)
                }
            }
        }
    }

    private fun setWarningColors(textColorRes: Int, bgColorRes: Int) {
        val textColor = ContextCompat.getColor(requireContext(), textColorRes)
        binding.tvWarningMessage.setTextColor(textColor)
        binding.tvWarningDetails.setTextColor(textColor)
        binding.layoutWarning.background = getWarningDrawable(bgColorRes)
    }

    private fun getWarningDrawable(colorRes: Int): GradientDrawable {
        val color = ContextCompat.getColor(requireContext(), colorRes)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = resources.getDimension(R.dimen.card_corner_radius)
            setColor(color)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}