package com.loteriavip.app.data.repository

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ParsingTest {

    private val COMPANY_CLASS_MAP = mapOf(
        "Leidsa" to "company-block-9",
        "Nacional" to "company-block-10",
        "Loteria Real" to "company-block-11",
        "Loteka" to "company-block-12",
        "Americanas" to "company-block-13",
        "La Primera" to "company-block-14",
        "La Suerte" to "company-block-15",
        "LoteDom" to "company-block-16",
        "Anguila" to "company-block-17",
        "King Lottery" to "company-block-18",
        "Nueva York" to "company-block-19",
        "Florida" to "company-block-20"
    )

    private fun buildApiDate(date: String?): String {
        if (date != null) {
            val parser = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val parsed = parser.parse(date)
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            iso.timeZone = TimeZone.getTimeZone("UTC")
            return iso.format(parsed!!)
        }
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        iso.timeZone = TimeZone.getTimeZone("UTC")
        return iso.format(Date())
    }

    @Test
    fun testLiveApiParsing() {
        val apiDate = buildApiDate(null)
        val apiUrl = "https://api.loteriasdominicanas.com/dominicana/sites/env?date=$apiDate"
        println("Testing URL: $apiUrl")
        
        val jsonText = URL(apiUrl).readText()
        val root = JSONObject(jsonText)
        val companies = root.getJSONArray("siteCompanies")
        
        val parsedResults = mutableListOf<String>()
        
        for (c in 0 until companies.length()) {
            val company = companies.getJSONObject(c)
            val companyTitle = company.getString("title")
            val siteGames = company.getJSONArray("siteGames")
            
            for (g in 0 until siteGames.length()) {
                val siteGame = siteGames.getJSONObject(g)
                val gameTitle = siteGame.getString("title")
                val game = siteGame.getJSONObject("game")
                val sessions = game.getJSONArray("sessions")
                
                for (s in 0 until sessions.length()) {
                    val session = sessions.getJSONObject(s)
                    val scoreArr = session.getJSONArray("score")
                    if (scoreArr.length() == 0) continue
                    
                    val numbers = mutableListOf<Int>()
                    for (i in 0 until scoreArr.length()) {
                        val inner = scoreArr.getJSONArray(i)
                        for (j in 0 until inner.length()) {
                            inner.getString(j).toIntOrNull()?.let { numbers.add(it) }
                        }
                    }
                    if (numbers.isEmpty()) continue
                    
                    val parsedDesc = "Company: $companyTitle, Game: $gameTitle, Numbers: $numbers"
                    println(parsedDesc)
                    parsedResults.add(parsedDesc)
                }
            }
        }
        
        assertFalse("Parsed results should not be empty", parsedResults.isEmpty())
        println("Successfully parsed ${parsedResults.size} games with numbers!")
    }
}
