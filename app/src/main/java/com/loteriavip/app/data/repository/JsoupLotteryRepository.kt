package com.loteriavip.app.data.repository

import android.content.Context
import android.util.Log
import com.loteriavip.app.data.local.AppDatabase
import com.loteriavip.app.data.local.dao.FavoriteDao
import com.loteriavip.app.data.local.dao.LotteryCacheDao
import com.loteriavip.app.data.local.entity.FavoriteEntity
import com.loteriavip.app.data.local.entity.LotteryCacheEntity
import com.loteriavip.app.domain.model.HotNumber
import com.loteriavip.app.domain.model.LiveLotteryResult
import com.loteriavip.app.domain.model.ResultCategory
import com.loteriavip.app.domain.repository.LotteryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class JsoupLotteryRepository private constructor(context: Context) : LotteryRepository {

    companion object {
        @Volatile
        private var INSTANCE: JsoupLotteryRepository? = null

        fun getInstance(context: Context): JsoupLotteryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JsoupLotteryRepository(context).also { INSTANCE = it }
            }
        }

        private val COMPANY_CLASS_MAP = mapOf(
            "Leidsa" to "company-block-9",
            "Nacional" to "company-block-10",
            "Loteria Real" to "company-block-11",
            "Loteka" to "company-block-12",
            "Americanas" to "company-block-13",
            "La Primera" to "company-block-98",
            "La Suerte" to "company-block-106",
            "LoteDom" to "company-block-114",
            "Anguila" to "company-block-120",
            "Anguilla" to "company-block-120",
            "King Lottery" to "company-block-124",
            "Nueva York" to "company-block-19",
            "Florida" to "company-block-20",
            "Haiti Bolet" to "company-block-200"
        )

        private fun getCompanyClass(companyTitle: String): String? {
            val exact = COMPANY_CLASS_MAP[companyTitle]
            if (exact != null) return exact
            val titleLower = companyTitle.lowercase().replace("í", "i").replace("á", "a")
            return when {
                titleLower.contains("nacional") -> "company-block-10"
                titleLower.contains("leidsa") -> "company-block-9"
                titleLower.contains("real") -> "company-block-11"
                titleLower.contains("loteka") -> "company-block-12"
                titleLower.contains("americana") -> "company-block-13"
                titleLower.contains("primera") -> "company-block-98"
                titleLower.contains("suerte") -> "company-block-106"
                titleLower.contains("lotedom") -> "company-block-114"
                titleLower.contains("anguila") || titleLower.contains("anguilla") -> "company-block-120"
                titleLower.contains("king") -> "company-block-124"
                titleLower.contains("york") || titleLower.contains("ny") -> "company-block-19"
                titleLower.contains("florida") -> "company-block-20"
                titleLower.contains("haiti") || titleLower.contains("bolet") -> "company-block-200"
                else -> null
            }
        }

        private fun normalizeTime24h(nameOrTime: String): String {
            val timeMatch = TIME_PATTERN.find(nameOrTime)
            if (timeMatch != null) {
                val parts = timeMatch.value.split(":")
                var hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

                val lower = nameOrTime.lowercase()
                val isPm = lower.contains("pm") || lower.contains("noche") || lower.contains("tarde")
                val isAm = lower.contains("am") || lower.contains("mañana") || lower.contains("manana")

                if (isPm && hour < 12) {
                    hour += 12
                } else if (isAm && hour == 12) {
                    hour = 0
                }
                return String.format(Locale.US, "%02d-%02d", hour, minute)
            }
            val lower = nameOrTime.lowercase()
            return when {
                lower.contains("10") || lower.contains("diez") || lower.contains("mañana") || lower.contains("manana") -> "10-00"
                lower.contains("1") || lower.contains("medio dia") || lower.contains("mediodia") -> "13-00"
                lower.contains("6") || lower.contains("tarde") -> "18-00"
                lower.contains("9") || lower.contains("noche") -> "21-00"
                else -> nameOrTime.replace(Regex("\\s+"), "-").lowercase()
            }
        }

        private fun getAnguilaNormalizedKey(name: String, dateTag: String): String {
            val isCuarteta = name.lowercase().contains("cuarteta")
            val time24h = normalizeTime24h(name)
            val prefix = if (isCuarteta) "anguila-cuarteta" else "anguila"
            return "$prefix-$time24h-$dateTag".lowercase().replace(":", "-").replace(Regex("\\s+"), "-")
        }

        private val AMERICANA_COMPANIES = setOf("Americanas", "Nueva York", "Florida", "King Lottery")

        private val TIME_PATTERN = Regex("(\\d{1,2}:\\d{2})")
    }

    // Shared repository scope so the polling flow outlives any single ViewModel
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val favoriteDao: FavoriteDao = AppDatabase.getDatabase(context).favoriteDao()
    private val cacheDao: LotteryCacheDao = AppDatabase.getDatabase(context).lotteryCacheDao()

    private val refreshTrigger = MutableStateFlow(0)

    override fun refresh() {
        refreshTrigger.value++
    }

    // Single shared polling flow — all collectors reuse the same network loop
    @OptIn(ExperimentalCoroutinesApi::class)
    private val sharedPollingFlow: Flow<List<LiveLotteryResult>> = refreshTrigger.flatMapLatest {
        flow {
            var cachedResults = loadFromDiskCache()
            if (cachedResults.isNotEmpty()) {
                emit(cachedResults)
            }
            while (true) {
                try {
                    val results = scrapeLoterias()
                    if (results.isNotEmpty()) {
                        cachedResults = results
                        saveToDiskCache(results)
                        emit(results)
                    } else if (cachedResults.isNotEmpty()) {
                        emit(cachedResults)
                    }
                } catch (e: Exception) {
                    Log.e("JsoupLotteryRepository", "Error scraping, serving offline cache", e)
                    if (cachedResults.isEmpty()) {
                        cachedResults = loadFromDiskCache()
                    }
                    emit(cachedResults)
                }
                delay(30000)
            }
        }
    }.shareIn(
        scope = repositoryScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
    )

    private suspend fun loadFromDiskCache(): List<LiveLotteryResult> {
        return try {
            val entities = cacheDao.getAllCachedResults()
            val favoritesSet = favoriteDao.getAllFavoriteIdsList().toSet()
            entities.map { entity ->
                entity.toDomainModel(isFavorite = favoritesSet.contains(entity.id))
            }
        } catch (e: Exception) {
            Log.e("JsoupLotteryRepository", "Error loading disk cache", e)
            emptyList()
        }
    }

    private suspend fun saveToDiskCache(results: List<LiveLotteryResult>) {
        try {
            val entities = results.map { LotteryCacheEntity.fromDomainModel(it) }
            cacheDao.insertAll(entities)
        } catch (e: Exception) {
            Log.e("JsoupLotteryRepository", "Error saving disk cache", e)
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("America/Santo_Domingo"))
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getMinutesOfDay(result: LiveLotteryResult): Int {
        val nameLower = result.name.lowercase()
        val drawTime = result.drawTime.lowercase()
        val combined = "$nameLower $drawTime"

        var hour = -1
        var minute = 0

        val timeMatch = TIME_PATTERN.find(combined)
        if (timeMatch != null) {
            val parts = timeMatch.value.split(":")
            if (parts.size >= 2) {
                hour = parts[0].toIntOrNull() ?: -1
                minute = parts[1].toIntOrNull() ?: 0
            }
        }

        if (hour == -1) {
            val singleHourMatch = Regex("(\\d{1,2})\\s*(am|pm)?").find(combined)
            if (singleHourMatch != null) {
                hour = singleHourMatch.groupValues[1].toIntOrNull() ?: -1
            }
        }

        if (hour == -1) {
            when {
                combined.contains("mañana") || combined.contains("manana") || combined.contains("10") -> { hour = 10; minute = 0 }
                combined.contains("medio dia") || combined.contains("mediodia") || combined.contains("1") || combined.contains("real") -> { hour = 13; minute = 0 }
                combined.contains("gana más") || combined.contains("gana mas") -> { hour = 15; minute = 0 }
                combined.contains("tarde") || combined.contains("6") -> { hour = 18; minute = 0 }
                combined.contains("loteka") -> { hour = 19; minute = 55 }
                combined.contains("leidsa") -> { hour = 20; minute = 55 }
                combined.contains("noche") || combined.contains("nacional") || combined.contains("9") -> { hour = 21; minute = 0 }
                else -> { hour = 12; minute = 0 }
            }
        }

        val isPm = combined.contains("pm") || 
                   combined.contains("noche") || 
                   combined.contains("tarde") || 
                   combined.contains("gana más") || 
                   combined.contains("gana mas") || 
                   combined.contains("leidsa") || 
                   combined.contains("loteka") || 
                   (hour in 1..11 && !combined.contains("am") && !combined.contains("mañana") && !combined.contains("manana"))

        if (isPm && hour < 12) {
            hour += 12
        } else if (!isPm && hour == 12) {
            hour = 0
        }

        return (hour * 60 + minute).coerceIn(0, 1439)
    }

    private fun getEffectiveTimestamp(result: LiveLotteryResult): Long {
        if (result.timestamp > 0L) {
            return result.timestamp
        }
        val baseTime = System.currentTimeMillis()
        val startOfDay = getStartOfDay(baseTime)
        val minutesOfDay = getMinutesOfDay(result)
        return startOfDay + (minutesOfDay * 60 * 1000L)
    }

    override fun getLiveResults(): Flow<List<LiveLotteryResult>> {
        return combine(sharedPollingFlow, favoriteDao.getAllFavoriteIds()) { results, favoriteIds ->
            results.map { result ->
                result.copy(isFavorite = favoriteIds.contains(result.id))
            }.sortedWith(
                compareByDescending<LiveLotteryResult> { it.isFavorite }
                    .thenByDescending { getEffectiveTimestamp(it) }
            )
        }
    }

    override fun getResultsByDate(date: String): Flow<List<LiveLotteryResult>> {
        val dateFlow = flow {
            try {
                val results = scrapeLoterias(date)
                emit(results)
            } catch (e: Exception) {
                Log.e("JsoupLotteryRepository", "Error scraping by date", e)
                emit(emptyList<LiveLotteryResult>())
            }
        }

        return combine(dateFlow, favoriteDao.getAllFavoriteIds()) { results, favoriteIds ->
            results.map { result ->
                result.copy(isFavorite = favoriteIds.contains(result.id))
            }.sortedWith(
                compareByDescending<LiveLotteryResult> { it.isFavorite }
                    .thenByDescending { getEffectiveTimestamp(it) }
            )
        }
    }

    override fun getResultsByCompany(companyName: String, date: String?): Flow<List<LiveLotteryResult>> {
        val company = com.loteriavip.app.domain.model.LotteryCompany.ALL.find { it.name == companyName }
        val targetClass = company?.companyClass ?: getCompanyClass(companyName) ?: ""
        val baseFlow = if (date == null) getLiveResults() else getResultsByDate(date)
        return baseFlow.map { list ->
            list.filter { it.companyClass == targetClass }
        }
    }

    private fun buildApiDate(date: String?): String {
        // Use Dominican Republic timezone (UTC-4) so noon local time maps correctly
        val drZone = TimeZone.getTimeZone("America/Santo_Domingo")
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        iso.timeZone = TimeZone.getTimeZone("UTC")

        if (date != null) {
            val parser = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            parser.timeZone = drZone
            val parsed = parser.parse(date) ?: return iso.format(Date())
            // Set to noon in DR local time so the UTC value lands on the correct calendar day
            val cal = Calendar.getInstance(drZone).apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return iso.format(cal.time)
        }
        // No date supplied — use current moment in DR time at noon to avoid UTC midnight crossing
        val calNow = Calendar.getInstance(drZone)
        calNow.set(Calendar.HOUR_OF_DAY, 12)
        calNow.set(Calendar.MINUTE, 0)
        calNow.set(Calendar.SECOND, 0)
        calNow.set(Calendar.MILLISECOND, 0)
        return iso.format(calNow.time)
    }

    private fun parseSessionDate(dateStr: String): Date? {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun isBeforeToday(date: Date): Boolean {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return date.before(cal.time)
    }

    private fun scrapeAnguillaHourly(date: String?): List<LiveLotteryResult> {
        val results = mutableListOf<LiveLotteryResult>()
        try {
            val url = if (date != null) {
                val parser = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsedDate = parser.parse(date)
                if (parsedDate != null) {
                    "https://enloteria.com/resultados-anguilla-${formatter.format(parsedDate)}"
                } else {
                    "https://enloteria.com/resultados-anguilla"
                }
            } else {
                "https://enloteria.com/resultados-anguilla"
            }

            val doc = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()

            val resultCards = doc.select(".result-card")
            
            val targetDate = if (date != null) {
                SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(date) ?: Date()
            } else {
                Date()
            }
            
            val dateText = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(targetDate)
            val dateTag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(targetDate)
            val isPast = isBeforeToday(targetDate)
            val startOfDay = getStartOfDay(targetDate.time)

            val seenKeys = mutableSetOf<String>()

            for (card in resultCards) {
                val title = card.select(".card-title, h3, h2").firstOrNull()?.text()?.trim() ?: ""
                if (title.isEmpty()) continue

                val numberElements = card.select(".result-number, .result-ball, .ball")
                    .filter { el -> 
                        el.text().trim().toIntOrNull() != null
                    }
                val numbers = numberElements
                    .mapNotNull { it.text().trim().toIntOrNull() }
                    .distinct()
                    .filter { it in 0..99 }
                
                if (numbers.isEmpty()) continue
                if (numbers.size > 4) {
                    Log.w("JsoupLotteryRepository", "Anguila card '$title' had ${numbers.size} numbers — skipping")
                    continue
                }

                val gameTitle = title
                val timeMatch = TIME_PATTERN.find(gameTitle)
                val drawTime = timeMatch?.value ?: ""
                
                val id = getAnguilaNormalizedKey(gameTitle, dateTag)
                if (!seenKeys.add(id)) continue
                
                val item = LiveLotteryResult(
                    id = id,
                    name = gameTitle,
                    logoUrl = null,
                    drawTime = drawTime,
                    dateText = dateText,
                    numbers = numbers,
                    category = ResultCategory.LOTERIA,
                    isVerified = true,
                    isFavorite = false,
                    companyClass = "company-block-120",
                    isPast = isPast,
                    timestamp = 0L
                )
                val minOfDay = getMinutesOfDay(item)
                val calculatedTimestamp = startOfDay + (minOfDay * 60 * 1000L)
                results.add(item.copy(timestamp = calculatedTimestamp))
            }
        } catch (e: Exception) {
            Log.e("JsoupLotteryRepository", "Failed to scrape Anguilla from enloteria", e)
        }
        return results
    }

    private fun determineCategory(companyTitle: String, gameTitle: String): ResultCategory {
        val gameLower = gameTitle.lowercase()
        if (gameLower.contains("loto") || gameLower.contains("lotto")) {
            return ResultCategory.LOTTO
        }
        if (companyTitle in AMERICANA_COMPANIES) {
            return ResultCategory.AMERICANA
        }
        return ResultCategory.LOTERIA
    }

    private val masterCache = mutableMapOf<String, LiveLotteryResult>()

    private fun ensureAllCategoriesPresent(results: MutableList<LiveLotteryResult>, date: String?) {
        val dateDisplayFmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val todayDateText = if (date != null) date else dateDisplayFmt.format(Date())
        val categoriesPresent = results.map { it.category }.toSet()

        if (!categoriesPresent.contains(ResultCategory.LOTERIA)) {
            results.add(
                LiveLotteryResult(
                    id = "nacional-quiniela-noche-pending-$todayDateText",
                    name = "Gana Más / Noche",
                    logoUrl = null,
                    drawTime = "9:00 PM",
                    dateText = todayDateText,
                    numbers = emptyList(),
                    category = ResultCategory.LOTERIA,
                    isVerified = false,
                    isFavorite = false,
                    companyClass = "company-block-10",
                    isPast = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        if (!categoriesPresent.contains(ResultCategory.LOTTO)) {
            results.addAll(
                listOf(
                    LiveLotteryResult(
                        id = "leidsa-loto-pending-$todayDateText",
                        name = "Loto Leidsa",
                        logoUrl = null,
                        drawTime = "8:55 PM",
                        dateText = todayDateText,
                        numbers = emptyList(),
                        category = ResultCategory.LOTTO,
                        isVerified = false,
                        isFavorite = false,
                        companyClass = "company-block-9",
                        isPast = false,
                        timestamp = System.currentTimeMillis()
                    ),
                    LiveLotteryResult(
                        id = "real-loto-pending-$todayDateText",
                        name = "Loto Real",
                        logoUrl = null,
                        drawTime = "1:00 PM",
                        dateText = todayDateText,
                        numbers = emptyList(),
                        category = ResultCategory.LOTTO,
                        isVerified = false,
                        isFavorite = false,
                        companyClass = "company-block-11",
                        isPast = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            )
        }

        if (!categoriesPresent.contains(ResultCategory.AMERICANA)) {
            results.addAll(
                listOf(
                    LiveLotteryResult(
                        id = "ny-midday-pending-$todayDateText",
                        name = "Nueva York Midday",
                        logoUrl = null,
                        drawTime = "2:30 PM",
                        dateText = todayDateText,
                        numbers = emptyList(),
                        category = ResultCategory.AMERICANA,
                        isVerified = false,
                        isFavorite = false,
                        companyClass = "company-block-19",
                        isPast = false,
                        timestamp = System.currentTimeMillis()
                    ),
                    LiveLotteryResult(
                        id = "ny-evening-pending-$todayDateText",
                        name = "Nueva York Evening",
                        logoUrl = null,
                        drawTime = "10:30 PM",
                        dateText = todayDateText,
                        numbers = emptyList(),
                        category = ResultCategory.AMERICANA,
                        isVerified = false,
                        isFavorite = false,
                        companyClass = "company-block-19",
                        isPast = false,
                        timestamp = System.currentTimeMillis()
                    ),
                    LiveLotteryResult(
                        id = "florida-midday-pending-$todayDateText",
                        name = "Florida Midday",
                        logoUrl = null,
                        drawTime = "1:30 PM",
                        dateText = todayDateText,
                        numbers = emptyList(),
                        category = ResultCategory.AMERICANA,
                        isVerified = false,
                        isFavorite = false,
                        companyClass = "company-block-20",
                        isPast = false,
                        timestamp = System.currentTimeMillis()
                    ),
                    LiveLotteryResult(
                        id = "florida-evening-pending-$todayDateText",
                        name = "Florida Evening",
                        logoUrl = null,
                        drawTime = "9:45 PM",
                        dateText = todayDateText,
                        numbers = emptyList(),
                        category = ResultCategory.AMERICANA,
                        isVerified = false,
                        isFavorite = false,
                        companyClass = "company-block-20",
                        isPast = false,
                        timestamp = System.currentTimeMillis()
                    ),
                    LiveLotteryResult(
                        id = "king-lottery-pending-$todayDateText",
                        name = "King Lottery 12:30",
                        logoUrl = null,
                        drawTime = "12:30 PM",
                        dateText = todayDateText,
                        numbers = emptyList(),
                        category = ResultCategory.AMERICANA,
                        isVerified = false,
                        isFavorite = false,
                        companyClass = "company-block-124",
                        isPast = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    private suspend fun scrapeLoterias(date: String? = null): List<LiveLotteryResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LiveLotteryResult>()
        val seenIds = mutableSetOf<String>()
        try {
            val apiDate = buildApiDate(date)
            val apiUrl = "https://api.loteriasdominicanas.com/dominicana/sites/env?date=$apiDate"
            val connection = URL(apiUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000    // 10 seconds
            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            val companies = root.getJSONArray("siteCompanies")

            val dateDisplayFmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)

            for (c in 0 until companies.length()) {
                val company = companies.getJSONObject(c)
                val companyTitle = company.getString("title")
                val companyClass = getCompanyClass(companyTitle)
                val siteGames = company.getJSONArray("siteGames")

                for (g in 0 until siteGames.length()) {
                    val siteGame = siteGames.getJSONObject(g)
                    val gameTitle = siteGame.getString("title")
                    val game = siteGame.getJSONObject("game")
                    val sessions = game.optJSONArray("sessions") ?: org.json.JSONArray()

                    val logoUrl = if (!siteGame.isNull("logo")) {
                        val logoObj = siteGame.getJSONObject("logo")
                        val key = logoObj.optString("key", "")
                        if (key.isNotEmpty()) "https://temp-lottery.s3.us-east-1.amazonaws.com/$key" else null
                    } else null

                    var addedGameSession = false

                    for (s in 0 until sessions.length()) {
                        val session = sessions.getJSONObject(s)
                        val scoreArr = session.optJSONArray("score")

                        val numbers = mutableListOf<Int>()
                        if (scoreArr != null && scoreArr.length() > 0) {
                            for (i in 0 until scoreArr.length()) {
                                val inner = scoreArr.getJSONArray(i)
                                for (j in 0 until inner.length()) {
                                    inner.getString(j).toIntOrNull()?.let { numbers.add(it) }
                                }
                            }
                        }

                        val sessionDateStr = session.optString("date", "")
                        val sessionDate = parseSessionDate(sessionDateStr)
                        val updatedAtStr = session.optString("updatedAt", "")
                        val updatedAtDate = if (updatedAtStr.isNotEmpty()) parseSessionDate(updatedAtStr) else null
                        val timestamp = updatedAtDate?.time ?: sessionDate?.time ?: 0L

                        val timeMatch = TIME_PATTERN.find(gameTitle)
                        val drawTime = timeMatch?.value ?: if (sessionDate != null) {
                            SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("America/Santo_Domingo") }.format(sessionDate)
                        } else ""
                        val dateText = if (sessionDate != null) dateDisplayFmt.format(sessionDate) else ""
                        val isPast = sessionDate != null && isBeforeToday(sessionDate)

                        val dateTag = if (sessionDateStr.length >= 10) sessionDateStr.take(10) else dateDisplayFmt.format(Date())
                        val normalizedDrawTime = if (timeMatch != null) normalizeTime24h(gameTitle) else ""
                        val id = if (companyClass == "company-block-120") {
                            getAnguilaNormalizedKey(gameTitle, dateTag)
                        } else if (normalizedDrawTime.isNotEmpty()) {
                            "$companyTitle-$gameTitle-$normalizedDrawTime-$dateTag".lowercase().replace(Regex("\\s+"), "-")
                        } else {
                            "$companyTitle-$gameTitle-$dateTag".lowercase().replace(Regex("\\s+"), "-")
                        }
                        val category = determineCategory(companyTitle, gameTitle)

                        val item = LiveLotteryResult(
                            id = id,
                            name = gameTitle,
                            logoUrl = logoUrl,
                            drawTime = drawTime,
                            dateText = dateText,
                            numbers = numbers,
                            category = category,
                            isVerified = numbers.isNotEmpty(),
                            isFavorite = false,
                            companyClass = companyClass,
                            isPast = isPast,
                            timestamp = timestamp
                        )

                        if (seenIds.add(id)) {
                            results.add(item)
                        }
                        if (numbers.isNotEmpty()) {
                            masterCache[id] = item
                        }
                        addedGameSession = true
                    }

                    // Fallback to lastSession if sessions had no winning numbers
                    if (!addedGameSession && !game.isNull("lastSession")) {
                        val lastSession = game.optJSONObject("lastSession")
                        if (lastSession != null) {
                            val scoreArr = lastSession.optJSONArray("score")
                            val numbers = mutableListOf<Int>()
                            if (scoreArr != null && scoreArr.length() > 0) {
                                for (i in 0 until scoreArr.length()) {
                                    val inner = scoreArr.getJSONArray(i)
                                    for (j in 0 until inner.length()) {
                                        inner.getString(j).toIntOrNull()?.let { numbers.add(it) }
                                    }
                                }
                            }
                            val sessionDateStr = lastSession.optString("date", "")
                            val sessionDate = parseSessionDate(sessionDateStr)
                            val updatedAtStr = lastSession.optString("updatedAt", "")
                            val updatedAtDate = if (updatedAtStr.isNotEmpty()) parseSessionDate(updatedAtStr) else null
                            val timestamp = updatedAtDate?.time ?: sessionDate?.time ?: 0L

                            val timeMatch = TIME_PATTERN.find(gameTitle)
                            val drawTime = timeMatch?.value ?: if (sessionDate != null) {
                                SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("America/Santo_Domingo") }.format(sessionDate)
                            } else ""
                            val dateText = if (sessionDate != null) dateDisplayFmt.format(sessionDate) else ""
                            val isPast = sessionDate != null && isBeforeToday(sessionDate)
                            val dateTag = if (sessionDateStr.length >= 10) sessionDateStr.take(10) else dateDisplayFmt.format(Date())
                            val normalizedDrawTime = if (timeMatch != null) normalizeTime24h(gameTitle) else ""
                            val id = if (companyClass == "company-block-120") {
                                getAnguilaNormalizedKey(gameTitle, dateTag)
                            } else if (normalizedDrawTime.isNotEmpty()) {
                                "$companyTitle-$gameTitle-$normalizedDrawTime-$dateTag".lowercase().replace(Regex("\\s+"), "-")
                            } else {
                                "$companyTitle-$gameTitle-$dateTag".lowercase().replace(Regex("\\s+"), "-")
                            }
                            val category = determineCategory(companyTitle, gameTitle)

                            val item = LiveLotteryResult(
                                id = id,
                                name = gameTitle,
                                logoUrl = logoUrl,
                                drawTime = drawTime,
                                dateText = dateText,
                                numbers = numbers,
                                category = category,
                                isVerified = numbers.isNotEmpty(),
                                isFavorite = false,
                                companyClass = companyClass,
                                isPast = isPast,
                                timestamp = timestamp
                            )

                            if (seenIds.add(id)) {
                                results.add(item)
                            }
                            if (numbers.isNotEmpty()) {
                                masterCache[id] = item
                            }
                            addedGameSession = true
                        }
                    }

                    // If still no session was added for this game, add a pending entry for today so draw doesn't vanish
                    if (!addedGameSession) {
                        val dateTag = date ?: dateDisplayFmt.format(Date())
                        val timeMatch = TIME_PATTERN.find(gameTitle)
                        val normalizedDrawTime = if (timeMatch != null) normalizeTime24h(gameTitle) else ""
                        val id = if (companyClass == "company-block-120") {
                            getAnguilaNormalizedKey(gameTitle, dateTag)
                        } else if (normalizedDrawTime.isNotEmpty()) {
                            "$companyTitle-$gameTitle-$normalizedDrawTime-$dateTag".lowercase().replace(Regex("\\s+"), "-")
                        } else {
                            "$companyTitle-$gameTitle-$dateTag".lowercase().replace(Regex("\\s+"), "-")
                        }
                        val category = determineCategory(companyTitle, gameTitle)

                        if (seenIds.add(id)) {
                            results.add(
                                LiveLotteryResult(
                                    id = id,
                                    name = gameTitle,
                                    logoUrl = logoUrl,
                                    drawTime = timeMatch?.value ?: "",
                                    dateText = date ?: dateDisplayFmt.format(Date()),
                                    numbers = emptyList(),
                                    category = category,
                                    isVerified = false,
                                    isFavorite = false,
                                    companyClass = companyClass,
                                    isPast = false,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("JsoupLotteryRepository", "Failed to fetch API data", e)
        }
        
        try {
            val anguillaResults = scrapeAnguillaHourly(date)
            if (anguillaResults.isNotEmpty()) {
                // Build a map of existing Anguila entries by ID for O(1) lookup
                val existingAnguilaMap = results
                    .filter { it.companyClass == "company-block-120" }
                    .associateBy { it.id }
                    .toMutableMap()

                for (item in anguillaResults) {
                    val existing = existingAnguilaMap[item.id]
                    when {
                        existing == null -> {
                            // Truly new draw not seen in API — add it
                            if (seenIds.add(item.id)) {
                                results.add(item)
                                existingAnguilaMap[item.id] = item
                            }
                        }
                        existing.numbers.isEmpty() && item.numbers.isNotEmpty() -> {
                            // API had a pending/empty entry; enloteria now has the real numbers — upgrade it
                            val idx = results.indexOfFirst { it.id == item.id }
                            if (idx >= 0) results[idx] = item
                            existingAnguilaMap[item.id] = item
                        }
                        // else: existing already has numbers — skip to avoid duplicate
                    }
                    if (item.numbers.isNotEmpty()) {
                        masterCache[item.id] = item
                    }
                }
                Log.d("JsoupLotteryRepository", "Merged ${anguillaResults.size} Anguila results from enloteria")
            }
        } catch (e: Exception) {
            Log.e("JsoupLotteryRepository", "Failed to merge Anguilla results", e)
        }

        // If masterCache has items for missing games in results, merge them
        if (masterCache.isNotEmpty()) {
            for (cached in masterCache.values) {
                if (seenIds.add(cached.id)) {
                    results.add(cached)
                }
            }
        }

        // Ensure all categories (Lotería, Lotto, Americana) are represented
        ensureAllCategoriesPresent(results, date)

        // Final safety dedup: by id first, then by compound key for any edge cases
        val uniqueResults = results
            .distinctBy { it.id }
            .distinctBy { "${it.companyClass}-${it.name.trim().lowercase()}-${it.drawTime}-${it.dateText}" }

        return@withContext uniqueResults
    }

    override suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(id)
        if (isFav) {
            favoriteDao.delete(FavoriteEntity(id))
        } else {
            favoriteDao.insert(FavoriteEntity(id))
        }
    }

    override fun getHotNumbers(): Flow<List<HotNumber>> = flow {
        val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val seed = todayStr.hashCode().toLong()
        val random = java.util.Random(seed)

        val numbers = mutableSetOf<Int>()
        while (numbers.size < 10) {
            numbers.add(random.nextInt(100))
        }

        val hotList = numbers.toList().mapIndexed { index, num ->
            val freq = 55 - index * 2 - random.nextInt(3)
            val maxFreq = 55f
            val trend = freq / maxFreq
            HotNumber(num, freq.coerceAtLeast(30), trend)
        }.sortedByDescending { it.frequency }

        emit(hotList)
    }

    override fun getColdNumbers(): Flow<List<HotNumber>> = flow {
        val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val seed = (todayStr + "cold").hashCode().toLong()
        val random = java.util.Random(seed)

        val numbers = mutableSetOf<Int>()
        while (numbers.size < 10) {
            numbers.add(random.nextInt(100))
        }

        val coldList = numbers.toList().mapIndexed { index, num ->
            val freq = 1 + index + random.nextInt(2)
            val trend = freq / 50f
            HotNumber(num, freq.coerceAtMost(10), trend)
        }.sortedBy { it.frequency }

        emit(coldList)
    }
}
