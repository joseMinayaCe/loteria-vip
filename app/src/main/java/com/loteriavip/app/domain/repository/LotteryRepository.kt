package com.loteriavip.app.domain.repository

import com.loteriavip.app.domain.model.HotNumber
import com.loteriavip.app.domain.model.LiveLotteryResult
import kotlinx.coroutines.flow.Flow

interface LotteryRepository {
    fun getLiveResults(): Flow<List<LiveLotteryResult>>
    fun getResultsByDate(date: String): Flow<List<LiveLotteryResult>>
    fun getResultsByCompany(companyName: String, date: String?): Flow<List<LiveLotteryResult>>
    suspend fun toggleFavorite(id: String)
    fun getHotNumbers(): Flow<List<HotNumber>>
    fun getColdNumbers(): Flow<List<HotNumber>>
    fun refresh()
}
