package com.loteriavip.app.presentation.screens.company

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loteriavip.app.data.repository.JsoupLotteryRepository
import com.loteriavip.app.domain.model.LiveLotteryResult
import com.loteriavip.app.domain.repository.LotteryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CompanyDrawsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LotteryRepository = JsoupLotteryRepository.getInstance(application)

    private val _companyName = MutableStateFlow<String>("")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val draws: StateFlow<List<LiveLotteryResult>> = combine(
        _companyName,
        _selectedDate
    ) { company, date -> Pair(company, date) }
        .flatMapLatest { (company, date) ->
            if (company.isEmpty()) return@flatMapLatest flowOf(emptyList())
            _isLoading.value = true
            repository.getResultsByCompany(company, date).onEach {
                _isLoading.value = false
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCompany(name: String) {
        _companyName.value = name
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun clearDate() {
        _selectedDate.value = null
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun refresh() {
        repository.refresh()
    }
}
