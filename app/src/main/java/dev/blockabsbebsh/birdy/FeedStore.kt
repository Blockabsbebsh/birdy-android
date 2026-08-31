package dev.blockabsbebsh.birdy

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class FeedStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("birdy", Context.MODE_PRIVATE)
    private val root = File(context.filesDir, "birdy")
    private val active = File(root, "active")

    fun cachedFeed(): BirdFeed? = preferences.getString(KEY_FEED, null)?.let { raw ->
        runCatching { BirdFeed.parse(raw) }.getOrNull()
    }

    fun language(): BirdLanguage = BirdLanguage.fromCode(
        preferences.getString(KEY_LANGUAGE, BirdLanguage.ENGLISH.code),
    )

    fun setLanguage(language: BirdLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun image(feed: BirdFeed, index: Int, family: String) =
        feed.birds.getOrNull(index)?.images?.get(family)?.let { filename ->
            val file = File(active, filename)
            if (file.isFile) BitmapFactory.decodeFile(file.absolutePath) else null
        }

    fun sync(): BirdFeed {
        root.mkdirs()
        val raw = downloadText("$FEED_URL?t=${System.currentTimeMillis()}")
        val incoming = BirdFeed.parse(raw)
        val existing = cachedFeed()
        if (existing?.generatedAt == incoming.generatedAt && active.isDirectory) return existing

        val staging = File(root, "staging-${System.currentTimeMillis()}")
        staging.mkdirs()
        try {
            incoming.birds.forEach { bird ->
                bird.images.values.forEach { filename ->
                    require(filename.matches(Regex("[A-Za-z0-9._-]+"))) { "Unsafe image filename" }
                    downloadFile(FEED_BASE + filename, File(staging, filename))
                }
            }
            val previous = File(root, "previous")
            previous.deleteRecursively()
            if (active.exists() && !active.renameTo(previous)) error("Could not preserve cached feed")
            if (!staging.renameTo(active)) {
                previous.renameTo(active)
                error("Could not activate new feed")
            }
            preferences.edit().putString(KEY_FEED, raw).apply()
            previous.deleteRecursively()
            return incoming
        } finally {
            if (staging.exists()) staging.deleteRecursively()
        }
    }

    private fun connection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Birdy-Android/1.0")
        }

    private fun downloadText(url: String): String = connection(url).run {
        try {
            if (responseCode !in 200..299) error("Feed returned HTTP $responseCode")
            inputStream.bufferedReader().use { it.readText() }
        } finally {
            disconnect()
        }
    }

    private fun downloadFile(url: String, destination: File) = connection(url).run {
        try {
            if (responseCode !in 200..299) error("Image returned HTTP $responseCode")
            inputStream.use { input -> destination.outputStream().use { input.copyTo(it) } }
            require(destination.length() > 0) { "Downloaded image is empty" }
        } finally {
            disconnect()
        }
    }

    companion object {
        const val FEED_URL = "https://blockabsbebsh.github.io/birdy-feed/latest.json"
        const val FEED_BASE = "https://blockabsbebsh.github.io/birdy-feed/"
        private const val KEY_FEED = "feed_json"
        private const val KEY_LANGUAGE = "language"
    }
}
