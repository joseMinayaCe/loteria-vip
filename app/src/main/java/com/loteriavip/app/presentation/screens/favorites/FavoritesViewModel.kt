package com.loteriavip.app.presentation.screens.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loteriavip.app.data.repository.JsoupLotteryRepository
import com.loteriavip.app.domain.model.LiveLotteryResult
import com.loteriavip.app.domain.repository.LotteryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LotteryRepository = JsoupLotteryRepository.getInstance(application)

    val favoriteResults: StateFlow<List<LiveLotteryResult>> = repository.getLiveResults()
        .map { results -> results.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun refresh() {
        repository.refresh()
    }
}
