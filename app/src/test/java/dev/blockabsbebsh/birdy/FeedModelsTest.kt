package dev.blockabsbebsh.birdy

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FeedModelsTest {
    private val feedJson = """
        {
          "generatedAt": "2026-08-29T00:00:00Z",
          "rotationMinutes": 60,
          "birds": [
            {"name":"One","sciName":"Primus","images":{"small":"1-s.jpg","medium":"1-m.jpg","large":"1-l.jpg"}},
            {"name":"Two","sciName":"Secundus","images":{"small":"2-s.jpg","medium":"2-m.jpg","large":"2-l.jpg"}}
          ]
        }
    """.trimIndent()

    @Test
    fun parsesFeedAndRotatesFromGenerationTime() {
        val feed = BirdFeed.parse(feedJson)

        assertEquals(2, feed.birds.size)
        assertEquals("1-m.jpg", feed.birds[0].images["medium"])
        assertEquals(0, feed.currentIndex(Instant.parse("2026-08-29T00:59:59Z")))
        assertEquals(1, feed.currentIndex(Instant.parse("2026-08-29T01:00:00Z")))
        assertEquals(0, feed.currentIndex(Instant.parse("2026-08-29T02:00:00Z")))
    }

    @Test
    fun olderFeedDefaultsLithuanianDisplayToEnglish() {
        val bird = BirdFeed.parse(feedJson).birds.first()

        assertEquals("One", bird.displayName(BirdLanguage.ENGLISH))
        assertEquals("One", bird.displayName(BirdLanguage.LITHUANIAN))
    }

    @Test
    fun parsesAndDisplaysLithuanianNameWhenAvailable() {
        val localized = feedJson.replace(
            "\"name\":\"One\"",
            "\"name\":\"One\",\"nameLt\":\"Pirmas\"",
        )
        val bird = BirdFeed.parse(localized).birds.first()

        assertEquals("One", bird.displayName(BirdLanguage.ENGLISH))
        assertEquals("Pirmas", bird.displayName(BirdLanguage.LITHUANIAN))
    }

    @Test
    fun usesLocalizedWikipediaArticleThenFallsBackToEnglish() {
        val localized = feedJson.replace(
            "\"sciName\":\"Primus\"",
            "\"sciName\":\"Primus\",\"wikipediaUrls\":{" +
                "\"en\":\"https://en.wikipedia.org/wiki/One\"," +
                "\"lt\":\"https://lt.wikipedia.org/wiki/Pirmas\"}",
        )
        val bird = BirdFeed.parse(localized).birds.first()

        assertEquals("https://en.wikipedia.org/wiki/One", bird.wikipediaUrl(BirdLanguage.ENGLISH))
        assertEquals("https://lt.wikipedia.org/wiki/Pirmas", bird.wikipediaUrl(BirdLanguage.LITHUANIAN))

        val englishOnly = localized.replace(
            ",\"lt\":\"https://lt.wikipedia.org/wiki/Pirmas\"",
            "",
        )
        assertEquals(
            "https://en.wikipedia.org/wiki/One",
            BirdFeed.parse(englishOnly).birds.first().wikipediaUrl(BirdLanguage.LITHUANIAN),
        )
    }

    @Test
    fun olderFeedHasNoWikipediaArticleUrl() {
        val bird = BirdFeed.parse(feedJson).birds.first()

        assertEquals(null, bird.wikipediaUrl(BirdLanguage.LITHUANIAN))
    }

    @Test
    fun calculatesNextRotationBoundary() {
        val feed = BirdFeed.parse(feedJson)

        assertEquals(
            30 * 60_000L,
            feed.nextRotationDelayMillis(Instant.parse("2026-08-29T00:30:00Z")),
        )
    }
}
