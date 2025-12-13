package com.enertrack.ui.calculate

import androidx.lifecycle.*
import com.enertrack.data.model.* // Import SubmitPayload, DevicePayload, Appliance, IotDevice
import com.enertrack.data.repository.CalculateRepository
import com.enertrack.data.repository.HistoryRepository
import com.enertrack.data.repository.Result
import com.enertrack.data.repository.onFailure
import com.enertrack.data.repository.onSuccess
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

class CalculateViewModel(
    private val calculateRepository: CalculateRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

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

    private val _submissionStatus = MutableLiveData<Result<SubmitResponseData>>()
    val submissionStatus: LiveData<Result<SubmitResponseData>> = _submissionStatus

    private var categoriesMap: Map<Int, String> = emptyMap()

    // --- REALTIME IoT DATA (FIRESTORE) ---
    // Kita pakai ini menggantikan simulasi lama kamu biar datanya asli
    private val db = Firebase.firestore

    private val _iotDevicesList = MutableLiveData<List<IotDevice>>()
    val iotDevicesList: LiveData<List<IotDevice>> = _iotDevicesList

    private val _selectedIotDevice = MutableLiveData<IotDevice?>()
    val selectedIotDevice: LiveData<IotDevice?> = _selectedIotDevice

    init {
        fetchCategoriesMap()
        startRealtimeMonitoring() // Panggil fungsi Firestore, bukan startIoTSimulation
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
                if (selectedHouseCapacity.value == null && data.isNotEmpty()) {
                    selectedHouseCapacity.value = data[0]
                }
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
        val finalCategoryName = categoriesMap[finalCategoryId] ?: "Electronics" // UPDATE: English Text

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

    // --- FUNGSI SUBMIT (MENGGUNAKAN LOGIKA LAMA YANG BENAR) ---
    fun submitDeviceList(houseCapacity: String, billingType: String) {
        val list = _applianceList.value
        if (list.isNullOrEmpty()) return

        val capacityValue = houseCapacity.replace(" VA", "").replace(".", "").toDoubleOrNull() ?: 0.0

        // 1. Mapping menggunakan DevicePayload (Bukan 'Device' buatan saya tadi)
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

        // 2. Bungkus menggunakan SubmitPayload (Bukan 'UserInput')
        // Struktur ini yang dimengerti oleh Repository kamu
        val payload = SubmitPayload(
            billingtype = billingType,
            electricity = mapOf("capacity" to capacityValue),
            devices = devicePayloadList
        )

        // 3. Kirim ke Repository
        viewModelScope.launch {
            calculateRepository.submitDevices(payload).onSuccess { data ->
                _submissionStatus.value = Result.Success(data)
                // UPDATE: Kosongkan list setelah sukses (Fitur Reset)
                _applianceList.value = emptyList()
            }.onFailure { e ->
                _submissionStatus.value = Result.Failure(e)
            }
        }
    }


    // === REALTIME MONITORING FUNCTION (DARI KODE BARU) ===
    fun startRealtimeMonitoring() {
        val currentUserId = 11 // TODO: Nanti ambil dari User Session asli

        db.collection("monitoring_live")
            .whereEqualTo("user_id", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val devices = ArrayList<IotDevice>()
                    for (doc in snapshots.documents) {
                        // Pastikan IotDevice punya empty constructor di data model
                        val device = doc.toObject(IotDevice::class.java)?.copy(docId = doc.id)
                        if (device != null) {
                            devices.add(device)
                        }
                    }
                    _iotDevicesList.value = devices

                    // Update detail view jika sedang dibuka
                    val currentSelection = _selectedIotDevice.value
                    if (currentSelection != null) {
                        val updatedDevice = devices.find { it.docId == currentSelection.docId }
                        if (updatedDevice != null) {
                            _selectedIotDevice.value = updatedDevice
                        }
                    }
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
}