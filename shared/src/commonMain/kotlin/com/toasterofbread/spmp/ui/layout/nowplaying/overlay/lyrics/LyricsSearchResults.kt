package com.toasterofbread.spmp.ui.layout.nowplaying.overlay.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toasterofbread.spmp.api.LyricsSearchResult
import com.toasterofbread.spmp.model.SongLyrics
import com.toasterofbread.spmp.platform.composable.BackHandler
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.theme.Theme
import com.toasterofbread.utils.getContrasted
import com.toasterofbread.utils.setAlpha

@Composable
internal fun ColumnScope.LyricsSearchResults(results: List<LyricsSearchResult>, modifier: Modifier = Modifier, onFinished: (Int?) -> Unit) {

    BackHandler {
        onFinished(null)
    }

    if (results.isNotEmpty()) {
        LazyColumn(
            modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(results.size + 1, { if (it == results.size) 0 else results[it].id }) {

                if (it == results.size) {
                    Text(getString("lyrics_no_more_results"), color = Theme.current.accent)
                }
                else {
                    val result = results[it]
                    Box(
                        Modifier
                            .background(Theme.current.accent, RoundedCornerShape(16))
                            .clickable {
                                onFinished(it)
                            }
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            @Composable
                            fun Item(name: String, value: String, colour: Color) {
                                Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(name.uppercase(), style = MaterialTheme.typography.bodySmall, color = colour)
                                    Text(value, color = colour)
                                }
                            }

                            val shape = RoundedCornerShape(16)

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(result.name, color = Theme.current.on_accent)

                                @Composable
                                fun text(text: String, colour: Color) {
                                    Text(
                                        text,
                                        Modifier.padding(5.dp),
                                        color = colour,
                                        fontSize = 10.sp,
                                        softWrap = false
                                    )
                                }

                                Spacer(Modifier.fillMaxWidth().weight(1f))

                                val sync_colour = if (result.sync_type == SongLyrics.SyncType.NONE) Color.LightGray else Color.Magenta
                                Box(Modifier.background(sync_colour, CircleShape)) {
                                    text(result.sync_type.readable, sync_colour.getContrasted())
                                }
                                Box(Modifier.background(result.source.colour, CircleShape)) {
                                    text(result.source.readable, result.source.colour.getContrasted())
                                }
                            }

                            Column(
                                Modifier
                                    .border(Dp.Hairline, Theme.current.on_accent, shape)
                                    .background(
                                        Theme.current
                                            .on_accent
                                            .setAlpha(0.1f), shape
                                    )
                                    .padding(2.dp)
                                    .fillMaxWidth()
                            ) {
                                if (result.artist_name != null) {
                                    Item(getString("artist"), result.artist_name!!, Theme.current.on_accent)
                                }
                                if (result.album_name != null) {
                                    Item(getString("album"), result.album_name!!, Theme.current.on_accent)
                                }
                            }
                        }
                    }
                }

            }
        }
    }
    else {
        Text("No results found", modifier, color = Theme.current.accent)
    }
}