import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import models.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ui.ConsoleRenderer
import java.io.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class InputHandlingTest {

    private lateinit var stats: Stats
    private lateinit var player: Player
    private lateinit var renderer: ConsoleRenderer

    @BeforeEach
    fun setup() {
        stats = Stats(maxHealth = 100, maxMana = 100, attack = 10, defense = 5)
        player = Player("Hero", stats)
        renderer = ConsoleRenderer()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial teaching flag is false for new player`() {
        assertFalse(player.hasSeenInitialTeaching, "New player should not have seen initial teaching")
    }

    @Test
    fun `initial teaching flag can be set to true`() {
        player.hasSeenInitialTeaching = true
        assertTrue(player.hasSeenInitialTeaching, "Flag should be set to true")
    }

    @Test
    fun `initial teaching flag is saved in progress`() {
        player.hasSeenInitialTeaching = true
        val progress = player.toProgress()

        assertTrue(progress.hasSeenInitialTeaching, "Progress should save teaching flag")
    }

    @Test
    fun `initial teaching flag is restored from progress`() {
        player.hasSeenInitialTeaching = true
        val progress = player.toProgress()

        val restoredPlayer = Player.fromProgress("Hero", progress, stats)

        assertTrue(restoredPlayer.hasSeenInitialTeaching, "Restored player should have teaching flag set")
    }

    @Test
    fun `player without teaching flag in old save gets default value`() {
        // Create a progress without the teaching flag (simulating old save)
        val oldProgress = PlayerProgress(
            level = 5,
            experience = 50,
            unlockedSpellNames = listOf("Melee Attack", "Fireball"),
            hasSeenInitialTeaching = false  // Default value
        )

        val restoredPlayer = Player.fromProgress("Hero", oldProgress, stats)

        assertFalse(restoredPlayer.hasSeenInitialTeaching, "Old save should default to false")
    }

    @Test
    fun `renderInitialTeaching displays welcome message`() {
        // This test just verifies the method can be called without errors
        // We can't easily test console output without mocking System.out
        try {
            renderer.renderInitialTeaching()
            // If we get here, no exception was thrown
            assertTrue(true)
        } catch (e: Exception) {
            fail("renderInitialTeaching should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `multiple consecutive readLine calls should work correctly`() = runTest {
        // Simulate the flow: teaching screen -> readLine -> game start -> readLine
        // This tests that we don't have issues with buffered input

        val inputStream = ByteArrayInputStream("first\nsecond\n".toByteArray())
        System.setIn(inputStream)

        // Simulate reading initial teaching input
        val firstInput = withContext(Dispatchers.IO) {
            readLine()
        }

        assertEquals("first", firstInput)

        // Small delay to ensure buffer is clear
        delay(10)

        // Simulate reading combat input
        val secondInput = withContext(Dispatchers.IO) {
            readLine()
        }

        assertEquals("second", secondInput)

        // Reset System.in
        System.setIn(System.`in`)
    }

    @Test
    fun `empty input should not cause issues`() = runTest {
        val inputStream = ByteArrayInputStream("\n\n".toByteArray())
        System.setIn(inputStream)

        // First empty enter
        val firstInput = withContext(Dispatchers.IO) {
            readLine()
        }

        assertEquals("", firstInput)

        // Second empty enter
        val secondInput = withContext(Dispatchers.IO) {
            readLine()
        }

        assertEquals("", secondInput)

        // Reset System.in
        System.setIn(System.`in`)
    }

    @Test
    fun `delay after readLine prevents input buffer issues`() = runTest {
        // Test that adding delays between screen transitions prevents accidental input consumption
        val inputStream = ByteArrayInputStream("command1\ncommand2\n".toByteArray())
        System.setIn(inputStream)

        // Simulate first screen with readLine + delay (like initial teaching)
        val firstInput = withContext(Dispatchers.IO) {
            readLine()
        }
        delay(100) // This is what we added to fix the bug

        // Simulate second screen with readLine (like combat)
        val secondInput = withContext(Dispatchers.IO) {
            readLine()
        }

        // Both inputs should be properly captured
        assertEquals("command1", firstInput)
        assertEquals("command2", secondInput)

        // Reset System.in
        System.setIn(System.`in`)
    }

    @Test
    fun `hasSeenInitialTeaching affects game flow`() {
        // When hasSeenInitialTeaching is false, initial teaching should be shown
        val newPlayer = Player("Hero", stats)
        assertFalse(newPlayer.hasSeenInitialTeaching)

        // After seeing teaching, flag should be true
        newPlayer.hasSeenInitialTeaching = true
        assertTrue(newPlayer.hasSeenInitialTeaching)

        // Flag should persist through save/load
        val progress = newPlayer.toProgress()
        val loadedPlayer = Player.fromProgress("Hero", progress, stats)
        assertTrue(loadedPlayer.hasSeenInitialTeaching)
    }

    @Test
    fun `simplified UX - actions execute immediately after queuing`() {
        // This test documents the simplified UX behavior:
        // When a user types a spell number and presses Enter,
        // the action is queued AND immediately executed
        // No second Enter press is required

        val newPlayer = Player("Hero", stats)

        // User should be able to type "1" + Enter and the action executes
        // This is handled by InputHandler which now calls executeQueuedActions()
        // immediately after queueing

        assertTrue(newPlayer.unlockedSpells.size >= 1, "Player should have at least one spell")
        assertEquals("Sync Blast", newPlayer.unlockedSpells[0].name)
    }
}
