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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class CalculateViewModel(
    private val calculateRepository: CalculateRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val TAG = "CalculateViewModel"
    private var firestoreListener: ListenerRegistration? = null

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
    private val db = Firebase.firestore
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

    // === REALTIME MONITORING FUNCTION (DINAMIS) ===
    fun startRealtimeMonitoring(userId: Int) {
        firestoreListener?.remove()
        Log.d(TAG, "Starting monitoring for user_id: $userId")

        firestoreListener = db.collection("monitoring_live")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val devices = ArrayList<IotDevice>()
                    for (doc in snapshots.documents) {
                        val data = doc.data
                        if (data == null) continue
                        try {
                            val device = IotDevice(
                                docId = doc.id,
                                user_id = (data["user_id"] as? Number)?.toInt() ?: 0,
                                device_name = data["device_name"] as? String ?: "Unknown",
                                status = data["status"] as? String ?: "OFFLINE",
                                watt = (data["watt"] as? Number)?.toDouble() ?: 0.0,
                                voltase = (data["voltase"] as? Number)?.toDouble() ?: 0.0,
                                ampere = (data["ampere"] as? Number)?.toDouble() ?: 0.0,
                                last_update = data["last_update"] as? Timestamp
                            )
                            devices.add(device)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Mapping error: ${doc.id}", ex)
                        }
                    }
                    _iotDevicesList.value = devices
                } else {
                    _iotDevicesList.value = emptyList()
                }
            }
    }

    fun selectDevice(device: IotDevice) {
        _selectedIotDevice.value = device
    }

    fun clearSelection() {
        _selectedIotDevice.value = null
    }

    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
    }
}