package com.loteriavip.app.data.repository

import com.loteriavip.app.domain.model.HotNumber
import com.loteriavip.app.domain.model.LiveLotteryResult
import com.loteriavip.app.domain.model.ResultCategory
import com.loteriavip.app.domain.repository.LotteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class MockLotteryRepository : LotteryRepository {

    private val _results = MutableStateFlow(listOf(
        LiveLotteryResult("1", "Gana Más", null, "2:30 PM", "04-06-2026", listOf(34, 12, 89), ResultCategory.LOTERIA, isVerified = true, companyClass = "company-block-10", isPast = false),
        LiveLotteryResult("2", "Lotería Nacional", null, "9:00 PM", "03-06-2026", listOf(5, 76, 21), ResultCategory.LOTERIA, isVerified = false, companyClass = "company-block-10", isPast = true),
        LiveLotteryResult("3", "Leidsa", null, "8:55 PM", "04-06-2026", listOf(10, 23, 45, 6, 11, 2), ResultCategory.LOTTO, isVerified = true, companyClass = "company-block-9", isPast = false),
        LiveLotteryResult("4", "New York Tarde", null, "2:30 PM", "04-06-2026", listOf(7, 43, 91), ResultCategory.AMERICANA, isVerified = true, companyClass = "company-block-13", isPast = false)
    ))

    override fun getLiveResults(): Flow<List<LiveLotteryResult>> = _results.asStateFlow()

    override fun getResultsByDate(date: String): Flow<List<LiveLotteryResult>> {
        return _results.asStateFlow().map { list ->
            list.map { it.copy(dateText = date) }
        }
    }

    override fun getResultsByCompany(companyName: String, date: String?): Flow<List<LiveLotteryResult>> {
        val company = com.loteriavip.app.domain.model.LotteryCompany.ALL.find { it.name == companyName }
        val targetClass = company?.companyClass ?: ""
        val baseFlow = if (date == null) getLiveResults() else getResultsByDate(date)
        return baseFlow.map { list ->
            list.filter { it.companyClass == targetClass }
        }
    }

    override suspend fun toggleFavorite(id: String) {
        _results.value = _results.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    override fun getHotNumbers(): Flow<List<HotNumber>> = kotlinx.coroutines.flow.flowOf(
        listOf(
            HotNumber(34, 45, 0.95f),
            HotNumber(12, 42, 0.88f),
            HotNumber(89, 39, 0.82f),
            HotNumber(5, 37, 0.78f),
            HotNumber(76, 35, 0.72f),
            HotNumber(21, 33, 0.68f),
            HotNumber(10, 31, 0.62f),
            HotNumber(23, 30, 0.60f)
        )
    )

    override fun getColdNumbers(): Flow<List<HotNumber>> = kotlinx.coroutines.flow.flowOf(
        listOf(
            HotNumber(99, 2, 0.05f),
            HotNumber(44, 3, 0.08f),
            HotNumber(15, 4, 0.12f),
            HotNumber(88, 5, 0.15f),
            HotNumber(62, 6, 0.18f),
            HotNumber(7, 7, 0.22f),
            HotNumber(50, 8, 0.25f),
            HotNumber(13, 9, 0.28f)
        )
    )

    override fun refresh() {
        // No-op for mock
    }
}
