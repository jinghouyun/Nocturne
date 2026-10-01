package com.nocturne.player.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
