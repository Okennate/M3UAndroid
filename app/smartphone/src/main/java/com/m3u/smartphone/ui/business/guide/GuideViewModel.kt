package com.m3u.smartphone.ui.business.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map as pagingMap
import com.m3u.business.playlist.ChannelWithProgramme
import com.m3u.data.database.model.Programme
import com.m3u.data.repository.channel.ChannelRepository
import com.m3u.data.repository.playlist.PlaylistRepository
import com.m3u.data.repository.programme.ProgrammeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GuideViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    playlistRepository: PlaylistRepository,
    programmeRepository: ProgrammeRepository,
) : ViewModel() {
    private val currentProgrammes: StateFlow<Map<String, Programme>> =
        playlistRepository.observePlaylistUrls()
            .flatMapLatest { playlistUrls ->
                flow {
                    while (currentCoroutineContext().isActive) {
                        val programmes = buildMap {
                            playlistUrls.forEach { playlistUrl ->
                                programmeRepository.getProgrammesCurrently(playlistUrl).forEach { (relationId, programme) ->
                                    put("$playlistUrl\u0000$relationId", programme)
                                }
                            }
                        }
                        emit(programmes)
                        delay(PROGRAMME_REFRESH_INTERVAL_MILLIS)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyMap(),
            )

    val channels: Flow<PagingData<ChannelWithProgramme>> = currentProgrammes
        .flatMapLatest { programmes ->
            Pager(
                config = PagingConfig(
                    pageSize = GUIDE_PAGE_SIZE,
                    enablePlaceholders = false,
                    prefetchDistance = GUIDE_PREFETCH_DISTANCE,
                ),
                pagingSourceFactory = { channelRepository.pagingAll("") },
            ).flow.map { page ->
                page.pagingMap { channel ->
                    ChannelWithProgramme(
                        channel = channel,
                        programme = channel.relationId?.let { relationId ->
                            programmes["${channel.playlistUrl}\u0000$relationId"]
                        },
                    )
                }.filter { item -> item.programme != null }
            }
        }
        .cachedIn(viewModelScope)

    private companion object {
        const val GUIDE_PAGE_SIZE = 30
        const val GUIDE_PREFETCH_DISTANCE = 8
        const val PROGRAMME_REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
