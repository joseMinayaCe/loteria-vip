package com.loteriavip.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.loteriavip.app.domain.model.LiveLotteryResult
import com.loteriavip.app.domain.model.ResultCategory

@Entity(tableName = "lottery_cache")
data class LotteryCacheEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String? = null,
    val drawTime: String = "",
    val dateText: String = "Hoy",
    val numbersStr: String = "", // Comma separated, e.g. "25,14,88"
    val categoryName: String = "LOTERIA",
    val isVerified: Boolean = false,
    val companyClass: String? = null,
    val isPast: Boolean = false,
    val timestamp: Long = 0L
) {
    fun toDomainModel(isFavorite: Boolean = false): LiveLotteryResult {
        val parsedCategory = try {
            ResultCategory.valueOf(categoryName)
        } catch (e: Exception) {
            ResultCategory.LOTERIA
        }
        val numbersList = if (numbersStr.isNotBlank()) {
            numbersStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else {
            emptyList()
        }
        return LiveLotteryResult(
            id = id,
            name = name,
            logoUrl = logoUrl,
            drawTime = drawTime,
            dateText = dateText,
            numbers = numbersList,
            category = parsedCategory,
            isVerified = isVerified,
            isFavorite = isFavorite,
            companyClass = companyClass,
            isPast = isPast,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomainModel(model: LiveLotteryResult): LotteryCacheEntity {
            return LotteryCacheEntity(
                id = model.id,
                name = model.name,
                logoUrl = model.logoUrl,
                drawTime = model.drawTime,
                dateText = model.dateText,
                numbersStr = model.numbers.joinToString(","),
                categoryName = model.category.name,
                isVerified = model.isVerified,
                companyClass = model.companyClass,
                isPast = model.isPast,
                timestamp = model.timestamp
            )
        }
    }
}
