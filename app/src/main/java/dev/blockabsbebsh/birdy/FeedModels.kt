package dev.blockabsbebsh.birdy

import org.json.JSONObject
import java.time.Instant

enum class BirdLanguage(val code: String, val label: String, val wikipediaLanguage: String) {
    ENGLISH("en", "English", "en"),
    LITHUANIAN("lt", "Lithuanian", "lt");

    companion object {
        fun fromCode(code: String?): BirdLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

data class Bird(
    val name: String,
    val lithuanianName: String,
    val scientificName: String,
    val wikipediaUrls: Map<String, String>,
    val images: Map<String, String>,
) {
    fun displayName(language: BirdLanguage): String = when (language) {
        BirdLanguage.ENGLISH -> name
        BirdLanguage.LITHUANIAN -> lithuanianName.ifBlank { name }
    }

    fun wikipediaUrl(language: BirdLanguage): String? =
        wikipediaUrls[language.wikipediaLanguage]?.takeIf { it.isNotBlank() }
            ?: wikipediaUrls[BirdLanguage.ENGLISH.wikipediaLanguage]?.takeIf { it.isNotBlank() }
}

data class BirdFeed(
    val generatedAt: Instant,
    val rotationMinutes: Long,
    val birds: List<Bird>,
) {
    fun currentIndex(now: Instant = Instant.now()): Int {
        if (birds.isEmpty()) return 0
        val elapsedMillis = (now.toEpochMilli() - generatedAt.toEpochMilli()).coerceAtLeast(0)
        val step = elapsedMillis / (rotationMinutes * 60_000L)
        return (step % birds.size).toInt()
    }

    fun nextRotationDelayMillis(now: Instant = Instant.now()): Long {
        val interval = rotationMinutes * 60_000L
        val elapsed = (now.toEpochMilli() - generatedAt.toEpochMilli()).coerceAtLeast(0)
        return (interval - elapsed % interval).coerceAtLeast(60_000L)
    }

    companion object {
        fun parse(raw: String): BirdFeed {
            val root = JSONObject(raw)
            val generatedAt = Instant.parse(root.getString("generatedAt"))
            val rotationMinutes = root.getLong("rotationMinutes")
            require(rotationMinutes > 0) { "Invalid rotation interval" }
            val entries = root.getJSONArray("birds")
            require(entries.length() > 0) { "Feed contains no birds" }
            val birds = buildList {
                for (index in 0 until entries.length()) {
                    val entry = entries.getJSONObject(index)
                    val imageObject = entry.getJSONObject("images")
                    val images = buildMap {
                        for (family in listOf("small", "medium", "large")) {
                            put(family, imageObject.getString(family))
                        }
                    }
                    val wikipediaObject = entry.optJSONObject("wikipediaUrls")
                    val wikipediaUrls = buildMap {
                        for (language in listOf("en", "lt")) {
                            wikipediaObject?.optString(language)?.takeIf { it.isNotBlank() }?.let {
                                put(language, it)
                            }
                        }
                    }
                    add(
                        Bird(
                            name = entry.getString("name"),
                            lithuanianName = entry.optString("nameLt"),
                            scientificName = entry.optString("sciName"),
                            wikipediaUrls = wikipediaUrls,
                            images = images,
                        )
                    )
                }
            }
            return BirdFeed(generatedAt, rotationMinutes, birds)
        }
    }
}
