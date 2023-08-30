package com.toasterofbread.spmp.model.mediaitem.playlist

import androidx.compose.runtime.Composable
import com.toasterofbread.spmp.model.mediaitem.AccountPlaylistRef
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.observeAsState
import com.toasterofbread.spmp.platform.PlatformContext

@Composable
fun rememberOwnedPlaylists(owner: Artist, context: PlatformContext): List<AccountPlaylistRef> {
    return context.database.playlistQueries.byOwner(owner.id)
        .observeAsState(
            {
                it.executeAsList().map { playlist_id ->
                    AccountPlaylistRef(playlist_id)
                }
            },
            null
        )
        .value
}