package com.example.musicapp.ui.audio

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.musicapp.data.local.model.Audio
import com.example.musicapp.data.repository.AudioRepository
import com.example.musicapp.player.service.JetAudioServiceHandler
import com.example.musicapp.player.service.JetAudioState
import com.example.musicapp.player.service.PlayerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val audioDummy = Audio(
    "".toUri(), "", 0L, "", "", 0, ""
)

@HiltViewModel
public class AudioViewModel @Inject public constructor(
    private val audioServiceHandler: JetAudioServiceHandler,
    private val repository: AudioRepository,
    savedStateHandler: SavedStateHandle
) : ViewModel() {

    var duration by savedStateHandler.saveable { mutableStateOf(0L) }
    var progress by savedStateHandler.saveable { mutableStateOf(0f) }
    var progressString by savedStateHandler.saveable { mutableStateOf("00.00") }
    var isPlaying by savedStateHandler.saveable { mutableStateOf(false) }
    var currentSelectedAudio by savedStateHandler.saveable { mutableStateOf(audioDummy) }
    var audioList by savedStateHandler.saveable { mutableStateOf(listOf<Audio>()) }

    private val _uiState: MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()


    init {
        loadAudioData()
    }

    init {

        viewModelScope.launch {
            audioServiceHandler.audioState.collectLatest { mediaState ->
                when (mediaState) {
                    JetAudioState.Initial -> _uiState.value = UIState.Initial
                    is JetAudioState.Buffering -> calculateProgressValue(mediaState.progress)
                    is JetAudioState.Playing -> isPlaying = mediaState.isPlaying
                    is JetAudioState.Progress -> calculateProgressValue(mediaState.progress)
                    is JetAudioState.CurrentPlaying -> {
                        currentSelectedAudio = audioList[mediaState.mediaItemIndex]
                    }

                    is JetAudioState.Ready -> {
                        duration = mediaState.duration
                        _uiState.value = UIState.Ready
                    }

                }


            }
        }
    }


    private fun loadAudioData() {
        viewModelScope.launch {
            val audio = repository.getAudioData()
            audioList = audio
            setMediaItems()
        }
    }


    private fun setMediaItems() {
        audioList.map { audio ->
            MediaItem.Builder()
                .setUri(audio.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setAlbumArtist(audio.artist)
                        .setDisplayTitle(audio.title)
                        .setSubtitle(audio.displayName)
                        .build()
                )
                .build()

        }.also {
            audioServiceHandler.setMediaItemList(it)
        }
    }

    private fun calculateProgressValue(currentProgress: Long) {
        progress =
            if (currentProgress > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f)
            else 0f
        progressString = formatDuration(currentProgress)
    }


    fun formatDuration(duration: Long): String {
        val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds = (minutes) - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
        return String.format("%02d:%02d", minutes, seconds)
    }


    override fun onCleared() {
        viewModelScope.launch {
            audioServiceHandler.onPlayerEvents(PlayerEvent.Stop)
        }
        super.onCleared()

    }

    fun onUiEvents(uiEvents: UIEvents) = viewModelScope.launch {
        when (uiEvents) {
            UIEvents.Backward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Backward)
            UIEvents.Forward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Forward)
            UIEvents.SeekToNext -> audioServiceHandler.onPlayerEvents(PlayerEvent.SeekToNext)
            is UIEvents.PlayPause -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.PlayPause
                )
            }

            is UIEvents.SeekTo -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.SeekTo,
                    seekPosition = ((duration * uiEvents.position) / 100f).toLong()
                )
            }

            is UIEvents.SelectedAudioChange -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.SelectedAudioChange,
                    selectedAudioIndex = uiEvents.index
                )
            }

            is UIEvents.UpdateProgress -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.UpdateProgress(
                        uiEvents.newProgress
                    )
                )

                progress = uiEvents.newProgress
            }
        }
    }
}

sealed class UIEvents {
    object PlayPause : UIEvents()
    data class SelectedAudioChange(val index: Int) : UIEvents()
    data class SeekTo(val position: Int) : UIEvents()
    object SeekToNext : UIEvents()
    object Backward : UIEvents()
    object Forward : UIEvents()
    data class UpdateProgress(val newProgress: Float) : UIEvents()
}

sealed class UIState {
    object Initial : UIState()
    object Ready : UIState()

}