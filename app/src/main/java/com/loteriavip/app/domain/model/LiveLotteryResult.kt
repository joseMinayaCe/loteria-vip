package com.loteriavip.app.domain.model

import com.squareup.moshi.Json

enum class ResultCategory {
    LOTERIA, LOTTO, AMERICANA
}

data class LiveLotteryResult(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val drawTime: String = "",
    @Json(name = "drawDate") val dateText: String = "Hoy",
    val numbers: List<Int> = emptyList(),
    val category: ResultCategory,
    val isVerified: Boolean = false,
    val isFavorite: Boolean = false,
    val companyClass: String? = null,
    val isPast: Boolean = false,
    val timestamp: Long = 0L
)

data class HotNumber(
    val number: Int,
    val frequency: Int,
    val trend: Float // 0.0 to 1.0
)
