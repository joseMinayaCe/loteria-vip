package com.loteriavip.app.presentation.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loteriavip.app.data.repository.JsoupLotteryRepository
import com.loteriavip.app.domain.model.HotNumber
import com.loteriavip.app.domain.repository.LotteryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LotteryRepository = JsoupLotteryRepository.getInstance(application)

    private val _isHotSelected = MutableStateFlow(true)
    val isHotSelected: StateFlow<Boolean> = _isHotSelected.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val statsNumbers: StateFlow<List<HotNumber>> = _isHotSelected.flatMapLatest { isHot ->
        _isLoading.value = true
        val flow = if (isHot) {
            repository.getHotNumbers()
        } else {
            repository.getColdNumbers()
        }
        flow.onEach {
            _isLoading.value = false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setHotSelected(isHot: Boolean) {
        _isHotSelected.value = isHot
    }
}
