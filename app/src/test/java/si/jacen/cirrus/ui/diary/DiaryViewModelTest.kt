package si.jacen.cirrus.ui.diary

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import si.jacen.cirrus.MainDispatcherRule
import si.jacen.cirrus.domain.model.Observation
import si.jacen.cirrus.domain.repository.FakeObservationRepository

class DiaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `observations reflect repository emissions`() = runTest {
        val observation = Observation(
            id = 1L,
            timestampMillis = 1_700_000_000_000L,
            cloudLabel = "cumulus",
            confidence = 0.91f,
            photoFileName = "a.jpg",
        )
        val repository = FakeObservationRepository(listOf(observation))
        val viewModel = DiaryViewModel(repository)

        val collected = mutableListOf<List<Observation>>()
        val collectionJob = backgroundScope.launch {
            viewModel.observations.toList(collected)
        }
        advanceUntilIdle()

        assertEquals(listOf(observation), collected.last())
        collectionJob.cancel()
    }

    @Test
    fun `starts with empty list before subscription`() = runTest {
        val viewModel = DiaryViewModel(FakeObservationRepository())

        assertEquals(emptyList<Observation>(), viewModel.observations.value)
    }
}
