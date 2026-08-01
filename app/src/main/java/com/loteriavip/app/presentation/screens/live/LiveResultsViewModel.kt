package com.loteriavip.app.presentation.screens.live

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loteriavip.app.data.repository.JsoupLotteryRepository
import com.loteriavip.app.domain.model.LiveLotteryResult
import com.loteriavip.app.domain.model.ResultCategory
import com.loteriavip.app.domain.repository.LotteryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LiveResultsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LotteryRepository = JsoupLotteryRepository.getInstance(application)

    private val _selectedCategory = MutableStateFlow(ResultCategory.LOTERIA)
    val selectedCategory: StateFlow<ResultCategory> = _selectedCategory.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val resultsFlow: Flow<List<LiveLotteryResult>> = combine(_selectedDate, _refreshTrigger) { date, _ -> date }
        .flatMapLatest { date ->
            _isLoading.value = true
            val flow = if (date == null) {
                repository.getLiveResults()
            } else {
                repository.getResultsByDate(date)
            }
            flow.onEach {
                _isLoading.value = false
            }.catch { e ->
                Log.e("LiveResultsViewModel", "Error in resultsFlow", e)
                _isLoading.value = false
                emit(emptyList())
            }
        }

    val filteredResults: StateFlow<List<LiveLotteryResult>> = combine(
        resultsFlow,
        _selectedCategory
    ) { results, category ->
        results.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(category: ResultCategory) {
        _selectedCategory.value = category
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
        _refreshTrigger.value++
    }
}
