package com.loteriavip.app.domain.model

data class LotteryCompany(
    val name: String,
    val companyClass: String,
    val color: Long,
    val hasHourlyDraws: Boolean = false
) {
    companion object {
        val ALL = listOf(
            LotteryCompany("Nacional",     "company-block-10",  0xFF3BB34AL, false),
            LotteryCompany("Leidsa",       "company-block-9",   0xFFFFB300L, false),
            LotteryCompany("Loteria Real", "company-block-11",  0xFF0D47A1L, false),
            LotteryCompany("Loteka",       "company-block-12",  0xFF00BCD4L, false),
            LotteryCompany("Anguila",      "company-block-120", 0xFFFF6D00L, true),
            LotteryCompany("King Lottery", "company-block-124", 0xFF0066CCL, true),
            LotteryCompany("La Primera",   "company-block-98",  0xFFE53935L, false),
            LotteryCompany("La Suerte",    "company-block-106", 0xFF1A237EL, false),
            LotteryCompany("LoteDom",      "company-block-114", 0xFF002FA7L, false),
            LotteryCompany("Americanas",   "company-block-13",  0xFF757575L, false),
            LotteryCompany("Nueva York",   "company-block-19",  0xFF1565C0L, false),
            LotteryCompany("Florida",      "company-block-20",  0xFFEF6C00L, false),
            LotteryCompany("Haiti Bolet",  "company-block-200", 0xFF8E24AAL, false)
        )
    }
}
