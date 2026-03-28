package com.rehearsall.ui.playlist

import androidx.lifecycle.SavedStateHandle
import com.rehearsall.data.repository.PlaylistRepository
import com.rehearsall.domain.model.Playlist
import com.rehearsall.domain.model.PlaylistItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelReorderTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var viewModel: PlaylistViewModel

    private val playlistId = 42L
    private val now = Instant.EPOCH
    private val playlist =
        Playlist(
            id = playlistId,
            name = "Test Playlist",
            trackCount = 3,
            totalDurationMs = 0L,
            createdAt = now,
            updatedAt = now,
        )

    private fun makeItem(
        id: Long,
        name: String,
        order: Int,
    ) = PlaylistItem(
        id = id,
        playlistId = playlistId,
        audioFileId = id,
        orderIndex = order,
        displayName = name,
        artist = null,
        durationMs = 60_000L,
        internalPath = "/data/$name.mp3",
        format = "mp3",
    )

    private val item1 = makeItem(1L, "Alpha", 0)
    private val item2 = makeItem(2L, "Beta", 1)
    private val item3 = makeItem(3L, "Gamma", 2)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        playlistRepository =
            mockk(relaxed = true) {
                coEvery { getById(playlistId) } returns playlist
                every { getPlaylistItems(playlistId) } returns flowOf(listOf(item1, item2, item3))
            }

        viewModel =
            PlaylistViewModel(
                savedStateHandle = SavedStateHandle(mapOf("playlistId" to playlistId)),
                playlistRepository = playlistRepository,
                playbackManager = mockk(relaxed = true),
            )

        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads items in order`() =
        runTest {
            val state = viewModel.uiState.value as PlaylistUiState.Loaded
            assertEquals(listOf(item1, item2, item3), state.items)
        }

    @Test
    fun `reorderItems calls repository with updated order pairs`() =
        runTest {
            // Move Alpha to end: Beta(0), Gamma(1), Alpha(2)
            viewModel.reorderItems(listOf(item2, item3, item1))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                playlistRepository.reorderItems(
                    playlistId = playlistId,
                    items = listOf(item2.id to 0, item3.id to 1, item1.id to 2),
                )
            }
        }

    @Test
    fun `reorderItems moving down one step calls repository correctly`() =
        runTest {
            // Move Alpha down one: Beta(0), Alpha(1), Gamma(2)
            viewModel.reorderItems(listOf(item2, item1, item3))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                playlistRepository.reorderItems(
                    playlistId = playlistId,
                    items = listOf(item2.id to 0, item1.id to 1, item3.id to 2),
                )
            }
        }

    @Test
    fun `reorderItems with same order does not crash`() =
        runTest {
            viewModel.reorderItems(listOf(item1, item2, item3))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                playlistRepository.reorderItems(
                    playlistId = playlistId,
                    items = any(),
                )
            }
        }

    @Test
    fun `reorderItems moving last item to first position`() =
        runTest {
            // Move Gamma to front: Gamma(0), Alpha(1), Beta(2)
            viewModel.reorderItems(listOf(item3, item1, item2))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                playlistRepository.reorderItems(
                    playlistId = playlistId,
                    items = listOf(item3.id to 0, item1.id to 1, item2.id to 2),
                )
            }
        }
}
