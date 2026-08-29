package dev.blockabsbebsh.birdy

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class BirdyWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = FeedStore(context)
        val feed = store.cachedFeed()
        provideContent {
            if (feed == null) LoadingWidget() else BirdWidget(store, feed)
        }
    }

    @Composable
    private fun BirdWidget(store: FeedStore, feed: BirdFeed) {
        val size = LocalSize.current
        val family = familyFor(size)
        val isPortrait = size.width.value / size.height.value < 0.82f
        val index = feed.currentIndex()
        val bird = feed.birds[index]
        val bitmap = store.image(feed, index, family)
        if (bitmap == null) {
            LoadingWidget()
            return
        }

        val query = URLEncoder.encode(bird.name, StandardCharsets.UTF_8.toString())
        val wikipedia = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://en.wikipedia.org/wiki/Special:Search?search=$query"),
        )

        Box(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF1C1C1C))),
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = bird.name,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = if (isPortrait) ContentScale.Fit else ContentScale.Crop,
            )
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0x96000000)))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = bird.name,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (family == "small") 14.sp else 17.sp,
                        ),
                        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(wikipedia)),
                        maxLines = if (family == "small") 2 else 1,
                    )
                    if (family != "small" && bird.scientificName.isNotBlank()) {
                        Text(
                            text = bird.scientificName,
                            style = TextStyle(
                                color = ColorProvider(Color(0xCDFFFFFF)),
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LoadingWidget() {
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(ColorProvider(Color(0xFF1C1C1C)))
                .clickable(actionRunCallback<RefreshAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🐦\nFetching birds…\nTap to retry",
                style = TextStyle(color = ColorProvider(Color.White)),
                maxLines = 3,
            )
        }
    }

    private fun familyFor(size: DpSize): String = when {
        size.width / size.height > 1.45f -> "medium"
        maxOf(size.width.value, size.height.value) >= 220f -> "large"
        else -> "small"
    }
}

class BirdyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirdyWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        BirdyScheduler.start(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        BirdyScheduler.start(context)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        BirdyScheduler.syncNow(context)
    }
}
