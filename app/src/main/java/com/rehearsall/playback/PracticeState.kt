package com.rehearsall.playback

import com.rehearsall.domain.model.PracticeStep

sealed interface PracticeState {
    data object Idle : PracticeState

    data class Playing(
        val currentStep: PracticeStep,
        val stepIndex: Int,
        val totalSteps: Int,
        val currentRep: Int,
        val totalReps: Int,
    ) : PracticeState

    data class Pausing(
        val nextStep: PracticeStep?,
        val stepIndex: Int,
        val totalSteps: Int,
    ) : PracticeState

    data object Complete : PracticeState
}
