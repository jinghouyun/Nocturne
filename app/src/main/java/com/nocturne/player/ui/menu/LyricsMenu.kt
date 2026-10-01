package com.nocturne.player.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nocturne.player.LocalDatabase
import com.nocturne.player.R
import com.nocturne.player.constants.LyricTrimKey
import com.nocturne.player.constants.MultilineLrcKey
import com.nocturne.player.db.entities.LyricsEntity
import com.nocturne.player.models.MediaMetadata
import com.nocturne.player.ui.component.PreferenceGroupTitle
import com.nocturne.player.ui.component.SettingsClickToReveal
import com.nocturne.player.ui.dialog.DefaultDialog
import com.nocturne.player.ui.dialog.TextFieldDialog
import com.nocturne.player.ui.screens.settings.fragments.LyricFormatFrag
import com.nocturne.player.ui.screens.settings.fragments.LyricParserFrag
import com.nocturne.player.ui.screens.settings.fragments.LyricSourceFrag
import com.nocturne.player.utils.rememberPreference
import com.nocturne.player.viewmodels.LyricsMenuViewModel
import org.akanework.gramophone.logic.utils.SemanticLyrics
import org.akanework.gramophone.logic.utils.parseLrc


@Composable
fun LyricsMenu(
    lyricsProvider: () -> Pair<LyricsEntity?, Boolean>,
    mediaMetadataProvider: () -> MediaMetadata,
    onDismiss: () -> Unit,
    viewModel: LyricsMenuViewModel = hiltViewModel(),
    onRefreshRequest: (SemanticLyrics?) -> Unit,
) {
    val database = LocalDatabase.current

    val multilineLrc by rememberPreference(MultilineLrcKey, defaultValue = true)
    val lyricTrim by rememberPreference(LyricTrimKey, defaultValue = false)

    val (lyrics, isDatabase) = lyricsProvider()

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        TextFieldDialog(
            onDismiss = { showEditDialog = false },
            icon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
            title = { Text(text = mediaMetadataProvider().title) },
            initialTextFieldValue = TextFieldValue(lyrics?.lyrics.orEmpty()),
            singleLine = false,
            onDone = {
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadataProvider().id,
                            lyrics = it
                        )
                    )
                }
                onRefreshRequest(parseLrc(it, lyricTrim, multilineLrc))
            }
        )
    }

    var showDeleteLyric by remember {
        mutableStateOf(false)
    }

    if (showDeleteLyric) {
        DefaultDialog(
            onDismiss = { showDeleteLyric = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_lyric_confirm, mediaMetadataProvider().title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeleteLyric = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showDeleteLyric = false
                        onDismiss()

                        lyricsProvider().first?.let {
                            database.query {
                                delete(it)
                            }
                        }
                        // refetch lyrics after database deletion. do not merge into one block.
                        lyricsProvider().first?.let {
                            onRefreshRequest(parseLrc(it.lyrics, lyricTrim, multilineLrc))
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    var showSettings by remember {
        mutableStateOf(false)
    }
    if (showSettings) {
        DefaultDialog(
            onDismiss = { showSettings = false },
            content = {
                Column() {
                    PreferenceGroupTitle(
                        title = stringResource(R.string.grp_lyrics_format)
                    )
                    LyricFormatFrag()

                    SettingsClickToReveal(stringResource(R.string.more_settings)) {
                        PreferenceGroupTitle(
                            title = stringResource(R.string.grp_lyrics_source)
                        )
                        LyricSourceFrag()

                        PreferenceGroupTitle(
                            title = stringResource(R.string.grp_lyrics_parser)
                        )
                        LyricParserFrag()
                    }
                }
            },
            buttons = {
                TextButton(
                    onClick = {
                        showSettings = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    var showAdjustDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var adjustBase by remember {
        mutableStateOf<String?>(null)
    }
    var adjustOffset by rememberSaveable {
        mutableStateOf(0L)
    }
    if (showAdjustDialog) {
        val base = adjustBase
        if (base != null) {
            LaunchedEffect(adjustOffset) {
                onRefreshRequest(parseLrc(applyLrcOffset(base, adjustOffset), lyricTrim, multilineLrc))
            }
            DefaultDialog(
                onDismiss = {
                    onRefreshRequest(parseLrc(base, lyricTrim, multilineLrc))
                    showAdjustDialog = false
                },
                icon = {
                    Icon(imageVector = Icons.Rounded.Timer, contentDescription = null)
                },
                title = {
                    Text(text = stringResource(R.string.adjust_lyric_time))
                },
                buttons = {
                    TextButton(onClick = {
                        onRefreshRequest(parseLrc(base, lyricTrim, multilineLrc))
                        showAdjustDialog = false
                    }) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                    TextButton(onClick = {
                        if (adjustOffset != readLrcOffset(base)) {
                            val newText = applyLrcOffset(base, adjustOffset)
                            database.query {
                                upsert(
                                    LyricsEntity(
                                        id = mediaMetadataProvider().id,
                                        lyrics = newText
                                    )
                                )
                            }
                        }
                        showAdjustDialog = false
                    }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            ) {
                val seconds = String.format(
                    java.util.Locale.US,
                    "%.1f",
                    kotlin.math.abs(adjustOffset) / 1000.0
                )
                Text(
                    text = when {
                        adjustOffset > 0 -> stringResource(R.string.lyric_time_earlier, seconds)
                        adjustOffset < 0 -> stringResource(R.string.lyric_time_later, seconds)
                        else -> stringResource(R.string.lyric_in_sync)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .align(Alignment.Start)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { adjustOffset += 100 }) {
                        Text("${stringResource(R.string.earlier)} 0.1")
                    }
                    TextButton(onClick = { adjustOffset += 500 }) {
                        Text("${stringResource(R.string.earlier)} 0.5")
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { adjustOffset -= 100 }) {
                        Text("${stringResource(R.string.later)} 0.1")
                    }
                    TextButton(onClick = { adjustOffset -= 500 }) {
                        Text("${stringResource(R.string.later)} 0.5")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { adjustOffset = 0L }) {
                        Text(text = stringResource(R.string.reset))
                    }
                }
            }
        }
    }

    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        GridMenuItem(
            icon = Icons.Rounded.Edit,
            title = R.string.edit
        ) {
            showEditDialog = true
        }
        GridMenuItem(
            icon = Icons.Rounded.SyncAlt,
            title = R.string.refetch
        ) {
            onDismiss()
            viewModel.refetchLyrics(mediaMetadataProvider()) { onRefreshRequest(it) }
        }

        GridMenuItem(
            icon = Icons.Rounded.Timer,
            title = R.string.adjust_lyric_time,
            enabled = lyrics != null
        ) {
            val raw = lyrics?.lyrics
            if (raw != null) {
                adjustBase = raw
                adjustOffset = readLrcOffset(raw)
                showAdjustDialog = true
            }
        }

        GridMenuItem(
            icon = Icons.Rounded.Delete,
            title = R.string.delete,
            enabled = isDatabase && lyrics != null
        ) {
            showDeleteLyric = true
        }

        GridMenuItem(
            icon = Icons.Rounded.Settings,
            title = R.string.settings,
        ) {
            showSettings = true
        }
    }
}

private val lrcOffsetLineRegex = Regex("(?im)^[ \\t]*\\[offset:[^\\]]*][ \\t]*\r?\n?")
private val lrcOffsetValueRegex = Regex("(?im)\\[offset:\\s*(-?\\d+)\\s*]")

private fun readLrcOffset(raw: String): Long =
    lrcOffsetValueRegex.find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

private fun applyLrcOffset(raw: String, newOffsetMs: Long): String {
    val removed = raw.replace(lrcOffsetLineRegex, "")
    return "[offset:$newOffsetMs]\n" + removed
}
