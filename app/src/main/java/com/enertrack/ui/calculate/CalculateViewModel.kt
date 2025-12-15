package com.enertrack.ui.calculate

import android.util.Log
import androidx.lifecycle.*
import com.enertrack.data.model.*
import com.enertrack.data.repository.CalculateRepository
import com.enertrack.data.repository.HistoryRepository
import com.enertrack.data.repository.Result
import com.enertrack.data.repository.onFailure
import com.enertrack.data.repository.onSuccess
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

class CalculateViewModel(
    private val calculateRepository: CalculateRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val TAG = "CalculateViewModel"

    // [MODIFIED] Ganti Firestore Listener jadi RTDB Listener
    private var rtdbListener: ValueEventListener? = null

    // [MODIFIED] Init RTDB dengan URL Asia Southeast (Sesuai backend kamu)
    private val rtdb = FirebaseDatabase.getInstance("https://enertrack-test-default-rtdb.asia-southeast1.firebasedatabase.app")

    // --- MANUAL CALC DATA ---
    private val _applianceList = MutableLiveData<List<Appliance>>(emptyList())
    val applianceList: LiveData<List<Appliance>> = _applianceList

    private val _houseCapacityOptions = MutableLiveData<List<String>>()
    val houseCapacityOptions: LiveData<List<String>> = _houseCapacityOptions
    val selectedHouseCapacity = MutableLiveData<String>()

    private val _brandOptions = MutableLiveData<List<String>>()
    val brandOptions: LiveData<List<String>> = _brandOptions

    private val _deviceOptionsForBrand = MutableLiveData<List<DeviceResponse>>()
    val deviceOptionsForBrand: LiveData<List<DeviceResponse>> = _deviceOptionsForBrand

    // [PENTING] Nullable agar bisa di-reset
    private val _submissionStatus = MutableLiveData<Result<SubmitResponseData>?>()
    val submissionStatus: LiveData<Result<SubmitResponseData>?> = _submissionStatus

    // Flag anti-spam (cadangan)
    private var isSubmitting = false

    private var categoriesMap: Map<Int, String> = emptyMap()

    // --- REALTIME IoT DATA ---
    private val _iotDevicesList = MutableLiveData<List<IotDevice>>()
    val iotDevicesList: LiveData<List<IotDevice>> = _iotDevicesList

    private val _selectedIotDevice = MutableLiveData<IotDevice?>()
    val selectedIotDevice: LiveData<IotDevice?> = _selectedIotDevice

    init {
        fetchCategoriesMap()
    }

    // --- FUNGSI HELPER MANUAL ---
    private fun getTariff(capacity: String?): Double {
        val capClean = capacity?.replace(" VA", "")?.replace(".", "") ?: "0"
        return when (capClean) {
            "900" -> 1352.0
            "1300" -> 1444.7
            "2200" -> 1444.7
            else -> 1444.7
        }
    }

    private fun fetchCategoriesMap() {
        viewModelScope.launch {
            calculateRepository.getCategories().onSuccess { categories ->
                categoriesMap = categories.associate { it.id to it.name }
            }
        }
    }

    fun fetchBrands() {
        viewModelScope.launch {
            calculateRepository.getBrands().onSuccess { data -> _brandOptions.value = data }
        }
    }

    fun fetchHouseCapacities() {
        viewModelScope.launch {
            calculateRepository.getHouseCapacities().onSuccess { data ->
                _houseCapacityOptions.value = data
                // Auto-select dimatikan agar hint muncul
            }
        }
    }

    fun fetchDevicesByBrand(brand: String) {
        viewModelScope.launch {
            calculateRepository.getDevicesByBrand(brand).onSuccess { data -> _deviceOptionsForBrand.value = data }
        }
    }

    fun addOrUpdateAppliance(name: String, brand: String, category: String?, powerStr: String, usageStr: String, qtyStr: String, categoryIdFromDevice: Int? = null) {
        val powerRating = powerStr.toDoubleOrNull() ?: 0.0
        val dailyUsage = usageStr.toDoubleOrNull() ?: 0.0
        if (dailyUsage <= 0) return

        val tariffPerKwh = getTariff(selectedHouseCapacity.value)
        val dailyEnergyKwh = (powerRating * dailyUsage) / 1000
        val dailyCost = dailyEnergyKwh * tariffPerKwh
        val currentId = System.currentTimeMillis()

        val finalCategoryId = categoryIdFromDevice ?: 1
        val finalCategoryName = categoriesMap[finalCategoryId] ?: "Electronics"

        val newAppliance = Appliance(
            id = currentId,
            name = name,
            brand = brand,
            category = finalCategoryName,
            category_id = finalCategoryId,
            powerRating = powerRating,
            dailyUsage = dailyUsage,
            quantity = 1,
            dailyEnergy = dailyEnergyKwh,
            weeklyEnergy = dailyEnergyKwh * 7,
            monthlyEnergy = dailyEnergyKwh * 30,
            dailyCost = dailyCost,
            weeklyCost = dailyCost * 7,
            monthlyCost = dailyCost * 30
        )

        val currentList = _applianceList.value.orEmpty().toMutableList()
        currentList.add(newAppliance)
        _applianceList.value = currentList
    }

    fun deleteAppliance(id: Long) {
        val currentList = _applianceList.value ?: emptyList()
        _applianceList.value = currentList.filter { it.id != id }
    }

    fun submitDeviceList(houseCapacity: String, billingType: String) {
        if (isSubmitting) return

        val list = _applianceList.value
        if (list.isNullOrEmpty()) return

        isSubmitting = true

        val capacityValue = houseCapacity.replace(" VA", "").replace(".", "").toDoubleOrNull() ?: 0.0

        val devicePayloadList = list.map { appliance ->
            DevicePayload(
                billingType = billingType,
                houseCapacity = houseCapacity,
                name = appliance.name,
                powerRating = appliance.powerRating,
                dailyUsage = appliance.dailyUsage,
                quantity = appliance.quantity,
                categoryId = appliance.category_id ?: 0,
                brand = appliance.brand ?: "Unknown"
            )
        }

        val payload = SubmitPayload(
            billingtype = billingType,
            electricity = mapOf("capacity" to capacityValue),
            devices = devicePayloadList
        )

        viewModelScope.launch {
            calculateRepository.submitDevices(payload).onSuccess { data ->
                _submissionStatus.value = Result.Success(data)
                _applianceList.value = emptyList()
                isSubmitting = false
            }.onFailure { e ->
                _submissionStatus.value = Result.Failure(e)
                isSubmitting = false
            }
        }
    }

    // [FIX] Reset status setelah pesan ditampilkan
    fun resetSubmissionStatus() {
        _submissionStatus.value = null
    }

    // === REALTIME MONITORING FUNCTION (DIRECT RTDB) ===
    // Kita bypass backend Railway supaya tidak kena timeout
    fun startRealtimeMonitoring(userId: Int) {
        // 1. Bersihkan listener lama jika ada
        rtdbListener?.let {
            rtdb.getReference("sensor").removeEventListener(it)
        }

        Log.d(TAG, "Starting DIRECT RTDB monitoring for user_id: $userId")

        // 2. Referensi langsung ke path "sensor" di RTDB
        val sensorRef = rtdb.getReference("sensor")

        rtdbListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        // 3. Ambil data manual sesuai struktur di RTDB/Arduino
                        // Field name harus sama persis dengan yang dikirim Arduino/simulasi (case sensitive)
                        val voltage = snapshot.child("voltage").getValue(Double::class.java) ?: 0.0
                        val ampere = snapshot.child("current").getValue(Double::class.java) ?: 0.0
                        val watt = snapshot.child("power").getValue(Double::class.java) ?: 0.0

                        // Logika status sederhana
                        val status = if (watt > 0.1) "ON" else "OFF"

                        // 4. Mapping ke object IotDevice
                        // DocID kita bikin dummy aja karena RTDB tidak punya Document ID kayak Firestore
                        val device = IotDevice(
                            docId = "sensor_utama_direct",
                            user_id = userId,
                            device_name = "Sensor Utama",
                            status = status,
                            watt = watt,
                            voltase = voltage,
                            ampere = ampere,
                            last_update = Timestamp.now()
                        )

                        // Update LiveData
                        _iotDevicesList.postValue(listOf(device))

                        // Update detail view jika sedang dipilih
                        if (_selectedIotDevice.value != null) {
                            _selectedIotDevice.postValue(device)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing RTDB data", e)
                    }
                } else {
                    // Data kosong atau path salah
                    _iotDevicesList.postValue(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "RTDB Listen failed: ${error.message}")
            }
        }

        // 5. Pasang listener
        sensorRef.addValueEventListener(rtdbListener!!)
    }

    fun selectDevice(device: IotDevice) {
        _selectedIotDevice.value = device
    }

    fun clearSelection() {
        _selectedIotDevice.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // [PENTING] Hapus listener saat ViewModel mati biar gak memory leak
        rtdbListener?.let {
            rtdb.getReference("sensor").removeEventListener(it)
        }
    }
}