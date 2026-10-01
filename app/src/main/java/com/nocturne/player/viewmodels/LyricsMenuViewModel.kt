package com.nocturne.player.viewmodels

import androidx.lifecycle.ViewModel
import com.nocturne.player.constants.LYRIC_FETCH_TIMEOUT
import com.nocturne.player.db.MusicDatabase
import com.nocturne.player.lyrics.LyricsHelper
import com.nocturne.player.lyrics.LyricsResult
import com.nocturne.player.models.MediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.akanework.gramophone.logic.utils.SemanticLyrics
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel @Inject constructor(
    private val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
) : ViewModel() {
    val results = MutableStateFlow(emptyList<LyricsResult>())
    val isLoading = MutableStateFlow(false)

    fun refetchLyrics(mediaMetadata: MediaMetadata, onDone: (SemanticLyrics?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            database.deleteLyricById(mediaMetadata.id)
            withTimeoutOrNull(LYRIC_FETCH_TIMEOUT) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                onDone(lyrics)
            }
        }
    }
}
