package com.toasterofbread.spmp.model.mediaitem.loader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.toasterofbread.spmp.model.mediaitem.Artist
import com.toasterofbread.spmp.model.mediaitem.ArtistData
import com.toasterofbread.spmp.model.mediaitem.MediaItem
import com.toasterofbread.spmp.model.mediaitem.MediaItemData
import com.toasterofbread.spmp.model.mediaitem.Playlist
import com.toasterofbread.spmp.model.mediaitem.PlaylistData
import com.toasterofbread.spmp.model.mediaitem.Song
import com.toasterofbread.spmp.model.mediaitem.SongData
import com.toasterofbread.spmp.platform.PlatformContext
import com.toasterofbread.spmp.ui.component.mediaitemlayout.MediaItemLayout
import com.toasterofbread.spmp.youtubeapi.EndpointNotImplementedException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object MediaItemLoader: ListenerLoader<String, MediaItemData>() {
    private val song_lock = ReentrantLock()
    private val artist_lock = ReentrantLock()
    private val playlist_lock = ReentrantLock()

    private val loading_songs: MutableMap<String, LoadJob<Result<SongData>>> = mutableMapOf()
    private val loading_artists: MutableMap<String, LoadJob<Result<ArtistData>>> = mutableMapOf()
    private val loading_playlists: MutableMap<String, LoadJob<Result<PlaylistData>>> = mutableMapOf()

    suspend fun <ItemType: MediaItemData> loadUnknown(item: ItemType, context: PlatformContext): Result<ItemType> =
        when (item) {
            is SongData -> loadSong(item, context) as Result<ItemType>
            is ArtistData -> loadArtist(item, context) as Result<ItemType>
            is PlaylistData -> loadPlaylist(item, context) as Result<ItemType>
            else -> throw NotImplementedError(item::class.toString())
        }

    suspend fun loadSong(song: SongData, context: PlatformContext): Result<SongData> {
        return loadItem(song, loading_songs, song_lock, context)
    }
    suspend fun loadArtist(artist: ArtistData, context: PlatformContext): Result<ArtistData> {
        return loadItem(artist, loading_artists, artist_lock, context)
    }
    suspend fun loadPlaylist(playlist: PlaylistData, context: PlatformContext, continuation: MediaItemLayout.Continuation? = null): Result<PlaylistData> {
        return loadItem(playlist, loading_playlists, playlist_lock, context, continuation)
    }

    fun isUnknownLoading(item: MediaItem): Boolean =
        when (item) {
            is Song -> isSongLoading(item)
            is Artist -> isArtistLoading(item)
            is Playlist -> isPlaylstLoading(item)
            else -> throw NotImplementedError(item::class.toString())
        }

    fun isSongLoading(song: Song): Boolean = song_lock.withLock {
        loading_songs.contains(song.id)
    }
    fun isArtistLoading(artist: Artist): Boolean = artist_lock.withLock {
        loading_artists.contains(artist.id)
    }
    fun isPlaylstLoading(playlist: Playlist): Boolean = playlist_lock.withLock {
        loading_playlists.contains(playlist.id)
    }

    override val listeners: MutableList<Listener<String, MediaItemData>> = mutableListOf()

    private suspend fun <ItemType: MediaItemData> loadItem(
        item: ItemType,
        loading_items: MutableMap<String, LoadJob<Result<ItemType>>>,
        lock: ReentrantLock,
        context: PlatformContext,
        continuation: MediaItemLayout.Continuation? = null
    ): Result<ItemType> {
        val result = performSafeLoad(
            item.id,
            lock,
            loading_items,
            listeners = listeners
        ) {
            val result = when (item) {
                is SongData -> {
                    with(context.ytapi.LoadSong) {
                        if (!isImplemented()) {
                            return@performSafeLoad Result.failure(EndpointNotImplementedException(this))
                        }
                        loadSong(item)
                    }
                }
                is ArtistData -> {
                    with(context.ytapi.LoadArtist) {
                        if (!isImplemented()) {
                            return@performSafeLoad Result.failure(EndpointNotImplementedException(this))
                        }
                        loadArtist(item)
                    }
                }
                is PlaylistData -> {
                    with(context.ytapi.LoadPlaylist) {
                        if (!isImplemented()) {
                            return@performSafeLoad Result.failure(EndpointNotImplementedException(this))
                        }
                        loadPlaylist(item, continuation)
                    }
                }
                else -> throw NotImplementedError(item::class.toString())
            }

            return@performSafeLoad result.fold(
                { Result.success(item) },
                { Result.failure(it) }
            )
        }
        return result
    }

}

@Composable
fun MediaItem.loadDataOnChange(context: PlatformContext, load: Boolean = true, onLoadFailed: ((Throwable?) -> Unit)? = null): State<Boolean> {
    val loading_state = remember(this) {
        mutableStateOf(MediaItemLoader.isUnknownLoading(this))
    }

    DisposableEffect(this) {
        val listener = object : ListenerLoader.Listener<String, MediaItemData> {
            override fun onLoadStarted(key: String) {
                if (key == id) {
                    loading_state.value = true
                }
            }
            override fun onLoadFinished(key: String, value: MediaItemData) {
                if (key == id) {
                    loading_state.value = false
                }
            }
            override fun onLoadFailed(key: String, error: Throwable) {
                if (key == id) {
                    onLoadFailed?.invoke(error)
                    loading_state.value = false
                }
            }
        }

        MediaItemLoader.addListener(listener)

        onDispose {
            MediaItemLoader.removeListener(listener)
        }
    }

    LaunchedEffect(this, load) {
        if (load) {
            onLoadFailed?.invoke(null)
            loadData(context)
        }
    }

    return loading_state
}