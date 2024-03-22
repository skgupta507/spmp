package com.toasterofbread.spmp.ui.layout.contentbar.element

import LocalPlayerState
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.toasterofbread.spmp.model.mediaitem.loader.SongLyricsLoader
import com.toasterofbread.spmp.model.mediaitem.song.Song
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import com.toasterofbread.spmp.ui.component.LyricsLineDisplay
import com.toasterofbread.composekit.utils.common.getValue
import kotlinx.serialization.Serializable

@Serializable
data class ContentBarElementLyrics(
    override val size_mode: ContentBarElement.SizeMode = DEFAULT_SIZE_MODE,
    override val size: Int = DEFAULT_SIZE,
): ContentBarElement() {
    override fun getType(): ContentBarElement.Type = ContentBarElement.Type.LYRICS

    override fun copyWithSize(size_mode: ContentBarElement.SizeMode, size: Int): ContentBarElement =
        copy(size_mode = size_mode, size = size)

    @Composable
    override fun ElementContent(vertical: Boolean, enable_interaction: Boolean, modifier: Modifier) {
        val player: PlayerState = LocalPlayerState.current
        val current_song: Song? by player.status.song_state

        val lyrics_state: SongLyricsLoader.ItemState? = remember(current_song?.id) { current_song?.let { SongLyricsLoader.getItemState(it, player.context) } }
        val lyrics_sync_offset: Long? by current_song?.getLyricsSyncOffset(player.database, true)

        Crossfade(
            lyrics_state?.lyrics,
            modifier
        ) { lyrics ->
            if (lyrics?.synced != true) {
                return@Crossfade
            }

            LyricsLineDisplay(
                lyrics = lyrics,
                getTime = {
                    (player.controller?.current_position_ms ?: 0) + (lyrics_sync_offset ?: 0)
                }
            )
        }
    }
}
