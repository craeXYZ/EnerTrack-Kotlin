package com.enertrack.ui.calculate

import androidx.lifecycle.*
import com.enertrack.data.model.*
import com.enertrack.data.repository.CalculateRepository
import com.enertrack.data.repository.HistoryRepository
import com.enertrack.data.repository.Result
import com.enertrack.data.repository.onFailure
import com.enertrack.data.repository.onSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlin.random.Random

class CalculateViewModel(
    private val calculateRepository: CalculateRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

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

    // === IOT SIMULATION DATA ===
    private val _iotVoltage = MutableLiveData<Double>()
    val iotVoltage: LiveData<Double> = _iotVoltage

    private val _iotCurrent = MutableLiveData<Double>()
    val iotCurrent: LiveData<Double> = _iotCurrent

    private val _iotPower = MutableLiveData<Int>()
    val iotPower: LiveData<Int> = _iotPower

    private val _iotStatus = MutableLiveData<String>()
    val iotStatus: LiveData<String> = _iotStatus

    private var simulationJob: Job? = null

    init {
        fetchCategoriesMap()
        startIoTSimulation()
    }

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
            calculateRepository.getBrands().onSuccess { data ->
                _brandOptions.value = data
            }
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
            calculateRepository.getDevicesByBrand(brand).onSuccess { data ->
                _deviceOptionsForBrand.value = data
            }.onFailure {
                _deviceOptionsForBrand.value = emptyList()
            }
        }
    }

    fun addOrUpdateAppliance(
        name: String, brand: String, category: String?,
        powerStr: String, usageStr: String, qtyStr: String,
        categoryIdFromDevice: Int? = null
    ) {
        val powerRating = powerStr.toDoubleOrNull() ?: 0.0
        val dailyUsage = usageStr.toDoubleOrNull() ?: 0.0
        if (dailyUsage <= 0) return

        val tariffPerKwh = getTariff(selectedHouseCapacity.value)
        val dailyEnergyKwh = (powerRating * dailyUsage) / 1000
        val dailyCost = dailyEnergyKwh * tariffPerKwh
        val currentId = System.currentTimeMillis()

        val finalCategoryId = categoryIdFromDevice ?: 1
        val finalCategoryName = categoriesMap[finalCategoryId] ?: "Elektronik"

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

        val newList = listOf(newAppliance)
        _applianceList.value = newList
    }

    fun deleteAppliance(id: Long) {
        val currentList = _applianceList.value ?: emptyList()
        _applianceList.value = currentList.filter { it.id != id }
    }

    fun submitDeviceList(houseCapacity: String, billingType: String) {
        val list = _applianceList.value
        if (list.isNullOrEmpty()) return

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
            }.onFailure { e ->
                _submissionStatus.value = Result.Failure(e)
            }
        }
    }

    // === IOT SIMULATION LOGIC ===
    private fun startIoTSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            _iotStatus.postValue("Connected")
            while (isActive) {
                val randomVoltage = 220.0 + Random.nextDouble(-5.0, 5.0)
                _iotVoltage.postValue(randomVoltage)

                val randomPower = Random.nextInt(400, 550)
                _iotPower.postValue(randomPower)

                val calculatedCurrent = randomPower / randomVoltage
                _iotCurrent.postValue(calculatedCurrent)

                delay(3000)
            }
        }
    }

    fun refreshIoTConnection() {
        viewModelScope.launch {
            simulationJob?.cancel()
            _iotStatus.value = "Connecting..."
            _iotPower.value = 0
            _iotVoltage.value = 0.0
            _iotCurrent.value = 0.0
            delay(2000)
            startIoTSimulation()
        }
    }
}