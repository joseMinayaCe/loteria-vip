package com.loteriavip.app.data.repository

import org.jsoup.Jsoup
import org.junit.Test
import org.junit.Assert.assertFalse

class EnLoteriaTest {

    @Test
    fun testScrapingAnguilla() {
        var url = "https://enloteria.com/resultados-anguilla"
        println("Fetching $url")
        var doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()

        var resultCards = doc.select(".result-card")
        var results = mutableListOf<String>()

        for (card in resultCards) {
            val title = card.select(".card-title, h3").text()
            val numberElements = card.select(".result-number, .result-ball")
            val numbers = numberElements.map { it.text().trim().toIntOrNull() }.filterNotNull()
            
            if (numbers.isNotEmpty()) {
                val parsedDesc = "Game: $title, Numbers: $numbers"
                println(parsedDesc)
                results.add(parsedDesc)
            }
        }
        
        // If empty (e.g. early morning), try yesterday's results
        if (results.isEmpty()) {
            url = "https://enloteria.com/resultados-anguilla-ayer"
            println("Fetching fallback $url")
            doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .get()
            resultCards = doc.select(".result-card")
            for (card in resultCards) {
                val title = card.select(".card-title, h3").text()
                val numberElements = card.select(".result-number, .result-ball")
                val numbers = numberElements.map { it.text().trim().toIntOrNull() }.filterNotNull()
                
                if (numbers.isNotEmpty()) {
                    val parsedDesc = "Game: $title, Numbers: $numbers"
                    println(parsedDesc)
                    results.add(parsedDesc)
                }
            }
        }
        
        println("Found ${results.size} games")
        assertFalse(results.isEmpty())
    }
}
