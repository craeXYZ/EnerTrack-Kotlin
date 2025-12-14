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
import com.enertrack.data.model.Appliance
import com.enertrack.data.model.IotDevice
import com.enertrack.databinding.BottomSheetAddDeviceBinding
import com.enertrack.databinding.FragmentCalculateBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.util.Locale

class CalculateFragment : Fragment() {

    private var _binding: FragmentCalculateBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CalculateViewModel
    private lateinit var applianceAdapter: ApplianceAdapter
    private lateinit var iotAdapter: IotDeviceAdapter

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

        // 1. Init ViewModel
        val factory = CalculateViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[CalculateViewModel::class.java]

        // 2. Setup Tampilan
        setupManualCalculatorAdapter()
        setupIoTAdapter()
        setupClickListeners()
        setupTabLayout()
        setupDropdowns()

        // 3. Ambil Data
        observeViewModel()
        viewModel.fetchHouseCapacities()
        viewModel.fetchBrands()

        // FIX: Panggil startRealtimeMonitoring() di sini juga,
        // agar listener langsung aktif saat fragment dibuat.
        viewModel.startRealtimeMonitoring()
    }

    // ==========================================
    // 1. LOGIKA LIST MANUAL & IOT
    // ==========================================

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

    // ==========================================
    // 2. LOGIKA BOTTOM SHEET
    // ==========================================

    private fun showAddDeviceBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetAddDeviceBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // A. Setup Dropdown Brand
        viewModel.brandOptions.observe(viewLifecycleOwner) { brands ->
            if (!brands.isNullOrEmpty()) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brands)
                sheetBinding.dropdownBrand.setAdapter(adapter)
                sheetBinding.dropdownBrand.setOnClickListener { sheetBinding.dropdownBrand.showDropDown() }
            }
        }

        // Kalau Brand Dipilih -> Ambil Device
        sheetBinding.dropdownBrand.setOnItemClickListener { parent, _, position, _ ->
            val selectedBrand = parent.getItemAtPosition(position).toString()
            sheetBinding.dropdownDevice.text = null
            sheetBinding.etPower.text = null
            sheetBinding.layoutDeviceName.isEnabled = false
            viewModel.fetchDevicesByBrand(selectedBrand)
        }

        // B. Setup Dropdown Device
        viewModel.deviceOptionsForBrand.observe(viewLifecycleOwner) { devices ->
            if (!devices.isNullOrEmpty()) {
                val deviceNames = devices.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, deviceNames)
                sheetBinding.dropdownDevice.setAdapter(adapter)
                sheetBinding.layoutDeviceName.isEnabled = true
                sheetBinding.dropdownDevice.setOnClickListener { sheetBinding.dropdownDevice.showDropDown() }
            }
        }

        // Kalau Device Dipilih -> Isi Watt Otomatis
        sheetBinding.dropdownDevice.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            val originalList = viewModel.deviceOptionsForBrand.value
            val selectedDeviceData = originalList?.find { it.name == selectedName }
            if (selectedDeviceData != null) {
                sheetBinding.etPower.setText(selectedDeviceData.powerWatt.toString())
            }
        }

        // C. Tombol Save di Bottom Sheet
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

        dialog.show()
    }

    // ==========================================
    // 3. UI LISTENERS & TABS
    // ==========================================

    private fun setupClickListeners() {
        binding.btnBackToList.setOnClickListener { showIoTListMode() }
        binding.btnRefreshIot.setOnClickListener {
            // FIX: Tambahkan indikator loading saat refresh
            binding.pbIotLoading.visibility = View.VISIBLE
            viewModel.startRealtimeMonitoring()
        }

        binding.fabAddDevice.setOnClickListener {
            showAddDeviceBottomSheet()
        }

        // --- TAMBAHAN: LOGIKA TOMBOL SAVE TO HISTORY ---
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
            Toast.makeText(requireContext(), "Saving to history...", Toast.LENGTH_SHORT).show()
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
                        // FIX: Memastikan monitoring dimulai dan loading ditunjukkan saat tab IoT dipilih
                        binding.pbIotLoading.visibility = View.VISIBLE
                        viewModel.startRealtimeMonitoring()
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

    // ==========================================
    // 4. OBSERVE DATA & HELPERS
    // ==========================================

    private fun observeViewModel() {
        // --- IoT Data ---
        viewModel.iotDevicesList.observe(viewLifecycleOwner) { devices ->
            // FIX: Selalu sembunyikan loading setelah data diterima (baik kosong atau ada)
            binding.pbIotLoading.visibility = View.GONE

            if (devices.isNullOrEmpty()) {
                binding.rvIotDevices.visibility = View.GONE
                binding.tvIotEmpty.visibility = View.VISIBLE
            } else {
                binding.rvIotDevices.visibility = View.VISIBLE
                binding.tvIotEmpty.visibility = View.GONE
                iotAdapter.setData(devices)
            }
        }

        viewModel.selectedIotDevice.observe(viewLifecycleOwner) { device ->
            if (device != null) {
                binding.tvDetailDeviceName.text = device.device_name
                binding.tvDetailDeviceInfo.text = "ID: ${device.docId}"

                when (device.status.uppercase()) {
                    "ON" -> {
                        binding.tvStatusLabel.text = "Status: Connected (Active)"
                        binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.energyGreen))
                    }
                    "OFF" -> {
                        binding.tvStatusLabel.text = "Status: Standby (OFF)"
                        binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.energyOrange))
                    }
                    else -> {
                        binding.tvStatusLabel.text = "Status: Offline"
                        binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
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

        // --- Manual Calc Data ---
        viewModel.houseCapacityOptions.observe(viewLifecycleOwner) { options ->
            val adapter = binding.dropdownCapacity.adapter as ArrayAdapter<String>
            adapter.clear()
            adapter.addAll(options)
        }

        // --- Update List Manual & Analisis Consumption ---
        viewModel.applianceList.observe(viewLifecycleOwner) { list ->
            applianceAdapter.submitList(list)

            if (list.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.cardSummary.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.cardSummary.visibility = View.VISIBLE
            }

            updateManualSummary(list)
            updateConsumptionAnalysis(list)
        }

        // --- Observer Status Submit ---
        viewModel.submissionStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is com.enertrack.data.repository.Result.Success -> {
                    Toast.makeText(requireContext(), "✅ Data saved successfully!", Toast.LENGTH_LONG).show()
                }
                is com.enertrack.data.repository.Result.Failure -> {
                    Toast.makeText(requireContext(), "❌ Failed to save: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        viewModel.selectedHouseCapacity.observe(viewLifecycleOwner) {
            val list = viewModel.applianceList.value ?: emptyList()
            updateConsumptionAnalysis(list)
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

    private fun formatNumber(value: Double): String {
        return NumberFormat.getInstance(Locale("id", "ID")).format(value)
    }

    private fun updateManualSummary(list: List<Appliance>) {
        val totalCost = list.sumOf { it.monthlyCost }
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        formatter.maximumFractionDigits = 0 // UPDATE: Hapus desimal koma (Rp 150.000)

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
    }
}