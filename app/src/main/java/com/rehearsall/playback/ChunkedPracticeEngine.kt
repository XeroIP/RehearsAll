package com.rehearsall.playback

import com.rehearsall.domain.model.PracticeSettings
import com.rehearsall.domain.model.PracticeStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the chunked practice session — iterates through steps,
 * counts reps via loop region enforcement, and pauses between reps/chunks.
 */
@Singleton
class ChunkedPracticeEngine @Inject constructor(
    private val playbackManager: PlaybackManager,
) {
    private val _state = MutableStateFlow<PracticeState>(PracticeState.Idle)
    val state: StateFlow<PracticeState> = _state.asStateFlow()

    private var practiceJob: Job? = null
    private var steps: List<PracticeStep> = emptyList()
    private var settings: PracticeSettings = PracticeSettings()
    private var currentStepIndex = 0

    fun startPractice(
        steps: List<PracticeStep>,
        settings: PracticeSettings,
        scope: CoroutineScope,
    ) {
        if (steps.isEmpty()) return
        stopPractice()

        this.steps = steps
        this.settings = settings
        this.currentStepIndex = 0

        practiceJob = scope.launch {
            runPractice()
        }
    }

    private suspend fun runPractice() {
        for (i in steps.indices) {
            currentStepIndex = i
            val step = steps[i]

            for (rep in 1..step.repeatCount) {
                _state.value = PracticeState.Playing(
                    currentStep = step,
                    stepIndex = i,
                    totalSteps = steps.size,
                    currentRep = rep,
                    totalReps = step.repeatCount,
                )

                // Set loop region and seek to start
                playbackManager.setLoopRegion(LoopRegion(step.startMs, step.endMs))
                playbackManager.seekTo(step.startMs)
                playbackManager.play()

                // Wait for this rep to complete (duration of the chunk)
                val chunkDuration = step.endMs - step.startMs
                val adjustedDuration = (chunkDuration / playbackManager.playbackState.value.speed.coerceAtLeast(0.25f)).toLong()
                delay(adjustedDuration)

                // Gap between reps (except after last rep)
                if (rep < step.repeatCount && settings.gapBetweenRepsMs > 0) {
                    playbackManager.pause()
                    delay(settings.gapBetweenRepsMs)
                }
            }

            // Gap between chunks (except after last chunk)
            if (i < steps.size - 1 && settings.gapBetweenChunksMs > 0) {
                playbackManager.pause()
                _state.value = PracticeState.Pausing(
                    nextStep = steps.getOrNull(i + 1),
                    stepIndex = i,
                    totalSteps = steps.size,
                )
                delay(settings.gapBetweenChunksMs)
            }
        }

        // Practice complete
        playbackManager.pause()
        playbackManager.clearLoopRegion()
        _state.value = PracticeState.Complete
        Timber.i("Practice session complete: %d steps", steps.size)
    }

    fun skipToNextStep() {
        val nextIndex = currentStepIndex + 1
        if (nextIndex < steps.size) {
            practiceJob?.cancel()
            currentStepIndex = nextIndex
            practiceJob = kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()
            ).launch {
                runFromStep(nextIndex)
            }
        }
    }

    fun skipToPreviousStep() {
        val prevIndex = (currentStepIndex - 1).coerceAtLeast(0)
        practiceJob?.cancel()
        currentStepIndex = prevIndex
        practiceJob = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()
        ).launch {
            runFromStep(prevIndex)
        }
    }

    private suspend fun runFromStep(startIndex: Int) {
        for (i in startIndex until steps.size) {
            currentStepIndex = i
            val step = steps[i]

            for (rep in 1..step.repeatCount) {
                _state.value = PracticeState.Playing(
                    currentStep = step,
                    stepIndex = i,
                    totalSteps = steps.size,
                    currentRep = rep,
                    totalReps = step.repeatCount,
                )

                playbackManager.setLoopRegion(LoopRegion(step.startMs, step.endMs))
                playbackManager.seekTo(step.startMs)
                playbackManager.play()

                val chunkDuration = step.endMs - step.startMs
                val adjustedDuration = (chunkDuration / playbackManager.playbackState.value.speed.coerceAtLeast(0.25f)).toLong()
                delay(adjustedDuration)

                if (rep < step.repeatCount && settings.gapBetweenRepsMs > 0) {
                    playbackManager.pause()
                    delay(settings.gapBetweenRepsMs)
                }
            }

            if (i < steps.size - 1 && settings.gapBetweenChunksMs > 0) {
                playbackManager.pause()
                _state.value = PracticeState.Pausing(
                    nextStep = steps.getOrNull(i + 1),
                    stepIndex = i,
                    totalSteps = steps.size,
                )
                delay(settings.gapBetweenChunksMs)
            }
        }

        playbackManager.pause()
        playbackManager.clearLoopRegion()
        _state.value = PracticeState.Complete
    }

    fun stopPractice() {
        practiceJob?.cancel()
        practiceJob = null
        if (_state.value !is PracticeState.Idle) {
            playbackManager.clearLoopRegion()
            _state.value = PracticeState.Idle
        }
    }
}
