package com.nocturne.player.ui.screens.library

import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.nocturne.player.LocalMenuState
import com.nocturne.player.LocalPlayerAwareWindowInsets
import com.nocturne.player.LocalPlayerConnection
import com.nocturne.player.LocalSnackbarHostState
import com.nocturne.player.MainActivity
import com.nocturne.player.R
import com.nocturne.player.constants.CONTENT_TYPE_HEADER
import com.nocturne.player.constants.CONTENT_TYPE_SONG
import com.nocturne.player.constants.ListThumbnailSize
import com.nocturne.player.constants.SongFilter
import com.nocturne.player.constants.SongFilterKey
import com.nocturne.player.constants.SongSortDescendingKey
import com.nocturne.player.constants.SongSortType
import com.nocturne.player.constants.SongSortTypeKey
import com.nocturne.player.constants.SwipeToQueueKey
import com.nocturne.player.db.entities.Song
import com.nocturne.player.models.toMediaMetadata
import com.nocturne.player.playback.queues.ListQueue
import com.nocturne.player.ui.component.ChipsRow
import com.nocturne.player.ui.component.EmptyPlaceholder
import com.nocturne.player.ui.component.FloatingFooter
import com.nocturne.player.ui.component.LazyColumnScrollbar
import com.nocturne.player.ui.component.ScrollToTopManager
import com.nocturne.player.ui.component.SelectHeader
import com.nocturne.player.ui.component.SortHeader
import com.nocturne.player.ui.component.items.SongListItem
import com.nocturne.player.ui.component.button.IconButton
import com.nocturne.player.ui.menu.ActionDropdown
import com.nocturne.player.ui.menu.DropdownItem
import com.nocturne.player.ui.utils.MEDIA_PERMISSION_LEVEL
import com.nocturne.player.utils.rememberEnumPreference
import com.nocturne.player.utils.rememberPreference
import com.nocturne.player.viewmodels.LibrarySongsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
    libraryFilterContent: @Composable (() -> Unit)? = null
) {
    Log.v("LibrarySongsScreen", "S_RC-1")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val snackbarHostState = LocalSnackbarHostState.current

    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIBRARY)
    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val swipeEnabled by rememberPreference(SwipeToQueueKey, true)

    val songs by viewModel.allSongs.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val lazyListState = rememberLazyListState()

    // multiselect
    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(songs) {
        selection.fastForEachReversed { songId ->
            if (songs?.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val filterContent = @Composable {
        // Salt Player doesn't show filter chips on the songs page
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Left: shuffle icon + song count
            IconButton(
                onClick = {
                    songs?.let { songList ->
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.queue_all_songs),
                                items = songList.map { it.toMediaMetadata() },
                                startShuffled = true,
                            )
                        )
                    }
                }
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null)
            }
            Spacer(Modifier.width(12.dp))
            songs?.let { songList ->
                Text(
                    text = "${songList.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.weight(1f))

            // Right: sort + view toggle
            IconButton(
                onClick = { onSortDescendingChange(!sortDescending) }
            ) {
                Icon(Icons.Rounded.Sort, contentDescription = null)
            }
            IconButton(
                onClick = { /* view toggle - list/grid */ }
            ) {
                Icon(Icons.Rounded.ViewList, contentDescription = null)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ScrollToTopManager(navController, lazyListState)
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.padding(bottom = if (inSelectMode) 64.dp else 0.dp)
        ) {
            item(
                key = "filter",
                contentType = CONTENT_TYPE_HEADER
            ) {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    var showStoragePerm by remember {
                        mutableStateOf(context.checkSelfPermission(MEDIA_PERMISSION_LEVEL) != PackageManager.PERMISSION_GRANTED)
                    }
                    if (showStoragePerm) {
                        TextButton(
                            onClick = {
                                showStoragePerm =
                                    false // allow user to hide error when clicked. This also makes the code a lot nicer too...
                                (context as MainActivity).permissionLauncher.launch(MEDIA_PERMISSION_LEVEL)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = stringResource(R.string.missing_media_permission_warning),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    libraryFilterContent?.let { it() } ?: filterContent()
                }
            }

            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER
            ) {
                headerContent()
            }

            songs?.let { songs ->
                if (songs.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = Icons.Rounded.MusicNote,
                            text = stringResource(R.string.library_song_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                val thumbnailSize = (ListThumbnailSize.value * density.density).roundToInt()
                itemsIndexed(
                    items = songs,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> CONTENT_TYPE_SONG }
                ) { index, song ->
                    SongListItem(
                        song = song,
                        navController = navController,
                        snackbarHostState = snackbarHostState,

                        isActive = song.song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        inSelectMode = inSelectMode,
                        isSelected = selection.contains(song.id),
                        onSelectedChange = {
                            inSelectMode = true
                            if (it) {
                                selection.add(song.id)
                            } else {
                                selection.remove(song.id)
                            }
                        },
                        swipeEnabled = swipeEnabled,

                        thumbnailSize = thumbnailSize,
                        onPlay = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = context.getString(R.string.queue_all_songs),
                                    items = songs.map { it.toMediaMetadata() },
                                    startIndex = index
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    )
                }
            }
        }
        LazyColumnScrollbar(
            state = lazyListState,
        )

        FloatingFooter(visible = inSelectMode && songs != null) {
            val s: List<Song> = (songs as Iterable<Song>).toList()
            SelectHeader(
                navController = navController,
                selectedItems = selection.mapNotNull { songId ->
                    s.find { it.id == songId }
                }.map { it.toMediaMetadata() },
                totalItemCount = s.size,
                onSelectAll = {
                    selection.clear()
                    selection.addAll(s.map { it.id })
                },
                onDeselectAll = { selection.clear() },
                menuState = menuState,
                onDismiss = onExitSelectionMode
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }
}
