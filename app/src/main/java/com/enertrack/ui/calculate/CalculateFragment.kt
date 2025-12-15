package com.enertrack.ui.calculate

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.enertrack.R
import com.enertrack.data.local.SessionManager
import com.enertrack.data.model.Appliance
import com.enertrack.data.model.IotDevice
import com.enertrack.databinding.BottomSheetAddDeviceBinding
import com.enertrack.databinding.FragmentCalculateBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.util.Locale

class CalculateFragment : Fragment() {

    private var _binding: FragmentCalculateBinding? = null
    private val binding get() = _binding!!

    private var activeSheetBinding: BottomSheetAddDeviceBinding? = null
    private var activeBottomSheetDialog: BottomSheetDialog? = null

    private lateinit var viewModel: CalculateViewModel
    private lateinit var applianceAdapter: ApplianceAdapter
    private lateinit var iotAdapter: IotDeviceAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = CalculateViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[CalculateViewModel::class.java]
        sessionManager = SessionManager(requireContext())

        setupManualCalculatorAdapter()
        setupIoTAdapter()
        setupClickListeners()
        setupTabLayout()
        setupDropdowns()
        setupUIUpdates()

        observeViewModel()
        viewModel.fetchHouseCapacities()
        viewModel.fetchBrands()

        startDynamicMonitoring()

    }

    private fun setupUIUpdates() {
        // Paksa icon panah back menjadi warna Biru (primary_accent)
        val backArrow = (binding.btnBackToList.getChildAt(0) as? android.widget.ImageView)
        backArrow?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_accent))

        // Pastikan textnya juga biru
        val backText = (binding.btnBackToList.getChildAt(1) as? android.widget.TextView)
        backText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_accent))
    }

    private fun startDynamicMonitoring() {
        val userIdStr = sessionManager.getUserId()
        val userId = userIdStr?.toIntOrNull()

        if (userId != null && userId != 0) {
            viewModel.startRealtimeMonitoring(userId)
        } else {
            binding.rvIotDevices.visibility = View.GONE
            binding.tvIotEmpty.text = "Please login to view devices"
            binding.tvIotEmpty.visibility = View.VISIBLE
            binding.pbIotLoading.visibility = View.GONE
        }
    }

    private fun setupManualCalculatorAdapter() {
        applianceAdapter = ApplianceAdapter(
            onDeleteClick = { id -> viewModel.deleteAppliance(id) }
        )
        binding.rvApplianceList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = applianceAdapter
        }
    }

    private fun setupIoTAdapter() {
        iotAdapter = IotDeviceAdapter { selectedDevice ->
            showIoTDetailMode(selectedDevice)
        }
        binding.rvIotDevices.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = iotAdapter
        }
    }

    private fun showAddDeviceBottomSheet() {
        val sheetBinding = BottomSheetAddDeviceBinding.inflate(layoutInflater)
        activeSheetBinding = sheetBinding

        val dialog = BottomSheetDialog(requireContext())
        activeBottomSheetDialog = dialog
        dialog.setContentView(sheetBinding.root)

        val currentBrands = viewModel.brandOptions.value
        if (!currentBrands.isNullOrEmpty()) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currentBrands)
            sheetBinding.dropdownBrand.setAdapter(adapter)
        }

        sheetBinding.dropdownBrand.setOnClickListener { sheetBinding.dropdownBrand.showDropDown() }
        sheetBinding.dropdownBrand.setOnItemClickListener { parent, _, position, _ ->
            val selectedBrand = parent.getItemAtPosition(position).toString()

            sheetBinding.dropdownDevice.text = null
            sheetBinding.dropdownDevice.setAdapter(null)
            sheetBinding.etPower.text = null
            sheetBinding.layoutDeviceName.isEnabled = false

            viewModel.fetchDevicesByBrand(selectedBrand)
        }

        sheetBinding.dropdownDevice.setOnClickListener { sheetBinding.dropdownDevice.showDropDown() }
        sheetBinding.dropdownDevice.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            val originalList = viewModel.deviceOptionsForBrand.value
            val selectedDeviceData = originalList?.find { it.name == selectedName }

            if (selectedDeviceData != null) {
                sheetBinding.etPower.setText(selectedDeviceData.powerWatt.toString())
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
                viewModel.addOrUpdateAppliance(
                    name = deviceName,
                    brand = brand,
                    category = null,
                    powerStr = powerStr,
                    usageStr = usageStr,
                    qtyStr = "1"
                )
                dialog.dismiss()
                Toast.makeText(requireContext(), "Device added!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setOnDismissListener {
            activeSheetBinding = null
            activeBottomSheetDialog = null
        }

        dialog.show()
    }

    private fun setupClickListeners() {
        binding.btnBackToList.setOnClickListener { showIoTListMode() }

        binding.fabAddDevice.setOnClickListener { showAddDeviceBottomSheet() }

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

            binding.btnSaveHistory.isEnabled = false
            binding.btnSaveHistory.text = "Saving..."

            viewModel.submitDeviceList(capacity, "Postpaid")
        }
    }

    private fun setupTabLayout() {
        binding.tabLayoutCalculate.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Manual
                        binding.calculatorContentContainer.visibility = View.VISIBLE
                        binding.iotContentContainer.visibility = View.GONE
                        binding.fabAddDevice.show()
                    }
                    1 -> { // IoT
                        binding.calculatorContentContainer.visibility = View.GONE
                        binding.iotContentContainer.visibility = View.VISIBLE
                        binding.fabAddDevice.hide()
                        showIoTListMode()
                        binding.pbIotLoading.visibility = View.VISIBLE
                        startDynamicMonitoring()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupDropdowns() {
        val capacityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        binding.dropdownCapacity.setAdapter(capacityAdapter)

        binding.dropdownCapacity.setOnItemClickListener { _, _, position, _ ->
            val selected = capacityAdapter.getItem(position) ?: return@setOnItemClickListener
            viewModel.selectedHouseCapacity.value = selected
        }
    }

    private fun observeViewModel() {
        viewModel.brandOptions.observe(viewLifecycleOwner) { brands ->
            activeSheetBinding?.let { sheet ->
                if (!brands.isNullOrEmpty()) {
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brands)
                    sheet.dropdownBrand.setAdapter(adapter)
                }
            }
        }

        viewModel.deviceOptionsForBrand.observe(viewLifecycleOwner) { devices ->
            activeSheetBinding?.let { sheet ->
                if (!devices.isNullOrEmpty()) {
                    val deviceNames = devices.map { it.name }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, deviceNames)
                    sheet.dropdownDevice.setAdapter(adapter)
                    sheet.layoutDeviceName.isEnabled = true
                    sheet.dropdownDevice.showDropDown()
                } else {
                    sheet.dropdownDevice.setAdapter(null)
                }
            }
        }

        viewModel.iotDevicesList.observe(viewLifecycleOwner) { devices ->
            binding.pbIotLoading.visibility = View.GONE
            if (devices.isNullOrEmpty()) {
                binding.rvIotDevices.visibility = View.GONE
                binding.tvIotEmpty.visibility = View.VISIBLE
            } else {
                binding.rvIotDevices.visibility = View.VISIBLE
                binding.tvIotEmpty.visibility = View.GONE
                iotAdapter.setData(devices)

                if (binding.layoutIotDetail.isVisible) {
                    val currentSelectedId = viewModel.selectedIotDevice.value?.docId
                    if (currentSelectedId != null) {
                        val updatedDeviceData = devices.find { it.docId == currentSelectedId }
                        if (updatedDeviceData != null) {
                            viewModel.selectDevice(updatedDeviceData)
                        }
                    }
                }
            }
        }

        viewModel.selectedIotDevice.observe(viewLifecycleOwner) { device ->
            if (device != null) {
                // [UPDATED] Persiapan Warna
                val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
                // Gunakan warna border yang sama dengan card Voltage/Current (grey_300 atau neutral_200)
                val borderColor = ContextCompat.getColor(requireContext(), R.color.grey_300)

                // --- CARD LOAD (Main Wattage / 54W) ---
                val cardLoad = binding.tvMainWatt.parent.parent as? MaterialCardView
                cardLoad?.apply {
                    setCardBackgroundColor(whiteColor)

                    // [IMPORTANT FIX] Hapus elevation (shadow) supaya "Surface Tint" (efek biru) hilang sepenuhnya
                    cardElevation = 0f

                    // [ADD] Tambahkan Stroke (Border) agar batasnya tetap terlihat rapi & konsisten
                    strokeWidth = (1 * resources.displayMetrics.density).toInt() // setara 1dp
                    strokeColor = borderColor
                }

                // --- CARD VOLTAGE ---
                val cardVoltage = binding.tvDetailVoltage.parent.parent as? MaterialCardView
                cardVoltage?.setCardBackgroundColor(whiteColor)

                // --- CARD CURRENT ---
                val cardCurrent = binding.tvDetailCurrent.parent.parent as? MaterialCardView
                cardCurrent?.setCardBackgroundColor(whiteColor)

                binding.tvDetailDeviceName.text = device.device_name
                binding.tvDetailDeviceInfo.text = "ID: ${device.docId}"

                // Ambil referensi ke card status di detail (Container Status Atas)
                val cardStatus = binding.tvStatusLabel.parent.parent.parent as? MaterialCardView

                val isHighVoltage = device.voltase > 250.0
                val displayStatus = when {
                    isHighVoltage -> "DANGER"
                    device.watt > 0 -> "ON"
                    else -> "OFF"
                }

                when (displayStatus) {
                    "DANGER" -> {
                        binding.tvStatusLabel.text = "Status: DANGER (Overvoltage)"
                        val solidRed = ContextCompat.getColor(requireContext(), R.color.dangerRed)
                        val bgRed = ContextCompat.getColor(requireContext(), R.color.bgLightRed)

                        binding.tvStatusLabel.setTextColor(solidRed)
                        cardStatus?.setCardBackgroundColor(bgRed)
                        val icon = (binding.tvStatusLabel.parent.parent as ViewGroup).getChildAt(0) as? android.widget.ImageView
                        icon?.setColorFilter(solidRed)
                    }
                    "ON" -> {
                        binding.tvStatusLabel.text = "Status: Active"
                        val solidGreen = ContextCompat.getColor(requireContext(), R.color.energyGreen)
                        val bgGreen = ContextCompat.getColor(requireContext(), R.color.bgLightGreen)

                        binding.tvStatusLabel.setTextColor(solidGreen)
                        cardStatus?.setCardBackgroundColor(bgGreen)

                        val icon = (binding.tvStatusLabel.parent.parent as ViewGroup).getChildAt(0) as? android.widget.ImageView
                        icon?.setColorFilter(solidGreen)
                    }
                    "OFF" -> {
                        binding.tvStatusLabel.text = "Status: Standby"
                        val solidOrange = ContextCompat.getColor(requireContext(), R.color.energyOrange)
                        val bgOrange = ContextCompat.getColor(requireContext(), R.color.bgLightOrange)

                        binding.tvStatusLabel.setTextColor(solidOrange)
                        cardStatus?.setCardBackgroundColor(bgOrange)

                        val icon = (binding.tvStatusLabel.parent.parent as ViewGroup).getChildAt(0) as? android.widget.ImageView
                        icon?.setColorFilter(solidOrange)
                    }
                }

                binding.tvMainWatt.text = "${formatNumber(device.watt)} W"
                binding.tvDetailVoltage.text = "${device.voltase} V"
                binding.tvDetailCurrent.text = "${device.ampere} A"

                val date = device.last_update?.toDate()
                if (date != null) {
                    val simpleFormat = java.text.SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())
                    binding.tvLastUpdate.text = "Last update: ${simpleFormat.format(date)}"
                } else {
                    binding.tvLastUpdate.text = "Last update: -"
                }
            }
        }

        viewModel.houseCapacityOptions.observe(viewLifecycleOwner) { options ->
            val adapter = binding.dropdownCapacity.adapter as ArrayAdapter<String>
            adapter.clear()
            adapter.addAll(options)
            adapter.notifyDataSetChanged()
        }

        viewModel.selectedHouseCapacity.observe(viewLifecycleOwner) { selected ->
            if (!selected.isNullOrEmpty() && binding.dropdownCapacity.text.toString() != selected) {
                binding.dropdownCapacity.setText(selected, false)
            }
            updateConsumptionAnalysis(viewModel.applianceList.value ?: emptyList())
        }

        viewModel.applianceList.observe(viewLifecycleOwner) { list ->
            applianceAdapter.submitList(list)
            updateManualSummary(list)
            updateConsumptionAnalysis(list)

            if (list.isNullOrEmpty()) {
                binding.cardSummary.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.tvTotalSummary.visibility = View.GONE
            } else {
                binding.cardSummary.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                binding.tvTotalSummary.visibility = View.VISIBLE
            }
        }

        viewModel.submissionStatus.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe

            binding.btnSaveHistory.isEnabled = true
            binding.btnSaveHistory.text = "Save to History"

            when (result) {
                is com.enertrack.data.repository.Result.Success -> {
                    Toast.makeText(requireContext(), "✅ Data saved successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.resetSubmissionStatus()
                }
                is com.enertrack.data.repository.Result.Failure -> {
                    Toast.makeText(requireContext(), "❌ Failed to save: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetSubmissionStatus()
                }
                else -> {}
            }
        }
    }

    private fun showIoTListMode() {
        viewModel.clearSelection()
        binding.layoutIotList.visibility = View.VISIBLE
        binding.layoutIotDetail.visibility = View.GONE
    }

    private fun showIoTDetailMode(device: IotDevice) {
        viewModel.selectDevice(device)
        binding.layoutIotList.visibility = View.GONE
        binding.layoutIotDetail.visibility = View.VISIBLE
    }

    private fun formatNumber(value: Double): String = NumberFormat.getInstance(Locale("id", "ID")).format(value)

    private fun updateManualSummary(list: List<Appliance>) {
        val totalCost = list.sumOf { it.monthlyCost }
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0

        binding.tvTotalSummary.text = "Total Est: ${formatter.format(totalCost)} / month"
        binding.tvTotalMonthlyCost.text = formatter.format(totalCost)
        val totalKwh = list.sumOf { it.dailyEnergy }
        binding.tvTotalDaily.text = String.format("%.2f", totalKwh)
    }

    private fun updateConsumptionAnalysis(list: List<Appliance>) {
        val capacityStr = viewModel.selectedHouseCapacity.value
            ?.replace(" VA", "")
            ?.replace(".", "")
            ?: "0"

        val capacity = capacityStr.toDoubleOrNull() ?: 0.0
        val totalPower = list.sumOf { it.powerRating }

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
        } else {
            binding.tvWarningMessage.text = "Select Capacity"
            binding.tvWarningDetails.text = "-"
            setWarningColors(R.color.text_secondary, R.color.neutral_100)
        }
    }

    private fun setWarningColors(textColorRes: Int, bgColorRes: Int) {
        val textColor = ContextCompat.getColor(requireContext(), textColorRes)
        val bgColor = ContextCompat.getColor(requireContext(), bgColorRes)

        binding.tvWarningMessage.setTextColor(textColor)
        binding.tvWarningDetails.setTextColor(textColor)
        binding.layoutWarning.background = getWarningDrawable(bgColor)
    }

    private fun getWarningDrawable(colorInt: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f * resources.displayMetrics.density
            setColor(colorInt)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        activeSheetBinding = null
        activeBottomSheetDialog = null
    }
}