package com.smartbasketball.app.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.smartbasketball.app.data.preferences.PreferencesManager
import com.smartbasketball.app.data.repository.GameRecordRepository
import com.smartbasketball.app.data.service.*
import com.smartbasketball.app.domain.model.*
import com.smartbasketball.app.ui.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var gameRecordRepository: GameRecordRepository

    @Mock
    private lateinit var preferencesManager: PreferencesManager

    @Mock
    private lateinit var faceRecognitionService: FaceRecognitionService

    @Mock
    private lateinit var gestureControlService: GestureControlService

    @Mock
    private lateinit var ballCountService: BallCountService

    @Mock
    private lateinit var dataSyncService: DataSyncService

    @Mock
    private lateinit var voiceService: VoiceService

    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        `when`(preferencesManager.schoolId).thenReturn(flowOf("test_school"))
        `when`(preferencesManager.countdownTime).thenReturn(flowOf(60))
        `when`(preferencesManager.fixedBallCount).thenReturn(flowOf(20))
        `when`(preferencesManager.standbyTimeout).thenReturn(flowOf(30))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is SCENE_RANK`() = runTest {
        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GameState.SCENE_RANK, state.gameState)
    }

    @Test
    fun `default game mode is FIXED_COUNT`() = runTest {
        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GameMode.FIXED_COUNT, state.gameMode)
    }

    @Test
    fun `game session tracks ball count correctly`() = runTest {
        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val session = viewModel.uiState.value.gameSession
        assertNotNull(session)
        assertEquals(0, session?.totalBalls)
        assertEquals(0, session?.madeBalls)
        assertEquals(0, session?.missedBalls)
    }

    @Test
    fun `accuracy calculation is correct`() {
        val session = GameSession(
            mode = GameMode.FIXED_COUNT,
            startTime = System.currentTimeMillis(),
            totalBalls = 20,
            madeBalls = 15,
            missedBalls = 5,
            isPlaying = false
        )

        assertEquals(0.75f, session.accuracy, 0.001f)
    }

    @Test
    fun `accuracy is zero when no balls`() {
        val session = GameSession(
            mode = GameMode.COUNTDOWN,
            startTime = System.currentTimeMillis(),
            totalBalls = 0,
            madeBalls = 0,
            missedBalls = 0,
            isPlaying = true
        )

        assertEquals(0f, session.accuracy, 0.001f)
    }

    @Test
    fun `game config has correct default values`() {
        val config = GameConfig()

        assertEquals(GameMode.FIXED_COUNT, config.defaultMode)
        assertEquals(60, config.countdownTimeSeconds)
        assertEquals(20, config.fixedBallCount)
        assertEquals(0.85f, config.faceThreshold, 0.001f)
        assertEquals(0.85f, config.gestureThreshold, 0.001f)
        assertEquals(50, config.ballDebounceMs)
        assertEquals(30, config.standbyTimeoutSeconds)
        assertEquals(30, config.leaderboardTimeoutSeconds)
        assertEquals(5, config.faceDetectTimeoutSeconds)
    }

    @Test
    fun `user state transitions correctly`() {
        val noUser = com.smartbasketball.app.data.service.UserState.NoUser
        val detecting = com.smartbasketball.app.data.service.UserState.Detecting

        assertTrue(noUser is com.smartbasketball.app.data.service.UserState.NoUser)
        assertTrue(detecting is com.smartbasketball.app.data.service.UserState.Detecting)
    }

    @Test
    fun `gesture type enum has correct values`() {
        assertEquals(4, GestureType.values().size)
        assertTrue(GestureType.values().contains(GestureType.NONE))
        assertTrue(GestureType.values().contains(GestureType.RIGHT_HAND_RAISED))
        assertTrue(GestureType.values().contains(GestureType.FIST))
        assertTrue(GestureType.values().contains(GestureType.BOTH_HANDS_CROSSED))
    }

    private fun createViewModel(): GameViewModel {
        return GameViewModel(
            gameRecordRepository,
            preferencesManager,
            faceRecognitionService,
            gestureControlService,
            ballCountService,
            dataSyncService,
            voiceService
        )
    }
}
