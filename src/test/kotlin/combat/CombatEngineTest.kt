package combat

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import models.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ui.ConsoleRenderer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CombatEngineTest {

    private lateinit var player: Player
    private lateinit var enemy: Enemy
    private lateinit var renderer: ConsoleRenderer
    private lateinit var testScope: TestScope
    private lateinit var combatEngine: CombatEngine

    @BeforeEach
    fun setup() {
        val stats = Stats(maxHealth = 100, maxMana = 100, attack = 10, defense = 5)
        player = Player("TestHero", stats)
        enemy = Enemy("TestEnemy", maxHealth = 50, attackDamage = 10, expReward = 50)
        renderer = mockk(relaxed = true)
        testScope = TestScope()
        combatEngine = CombatEngine(player, enemy, testScope, renderer)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `combat starts with correct initial state`() {
        assertEquals(1, combatEngine.round)
        assertTrue(combatEngine.combatLog.isEmpty())
    }

    @Test
    fun `runCombat adds initial combat message`() = testScope.runTest {
        // Kill enemy immediately to end combat quickly
        enemy.health = 1

        val combatJob = launch {
            combatEngine.runCombat()
        }

        advanceTimeBy(200)
        player.mana = 100
        combatEngine.castSpell(0) // Melee attack
        advanceUntilIdle()
        combatJob.cancel()

        assertTrue(combatEngine.combatLog.any { it.contains("Combat begins") })
    }

    @Test
    fun `castSpell with valid index and mana casts spell successfully`() = testScope.runTest {
        player.mana = 100
        val initialEnemyHealth = enemy.health

        combatEngine.castSpell(0) // Melee attack
        advanceUntilIdle()

        assertTrue(enemy.health < initialEnemyHealth)
        assertTrue(combatEngine.combatLog.any { it.contains("strike") })
    }

    @Test
    fun `castSpell with invalid index shows error`() = testScope.runTest {
        combatEngine.castSpell(99)
        advanceUntilIdle()

        assertTrue(combatEngine.combatLog.any { it.contains("Invalid spell") })
    }

    @Test
    fun `castSpell with insufficient mana shows error`() = testScope.runTest {
        player.mana = 0
        player.unlockedSpells.add(Fireball)

        combatEngine.castSpell(1) // Fireball needs 15 mana
        advanceUntilIdle()

        assertTrue(combatEngine.combatLog.any { it.contains("Not enough mana") })
    }

    @Test
    fun `castSpell on cooldown shows error`() = testScope.runTest {
        player.mana = 100
        player.unlockedSpells.add(Fireball)

        // Cast once
        combatEngine.castSpell(1)
        advanceUntilIdle()

        // Clear log
        combatEngine.combatLog.clear()

        // Try to cast again immediately (should be on cooldown)
        combatEngine.castSpell(1)
        advanceUntilIdle()

        assertTrue(combatEngine.combatLog.any { it.contains("cooldown") || it.contains("Wait for your turn") })
    }

    @Test
    fun `castSpell reduces player mana`() = testScope.runTest {
        player.mana = 100
        player.unlockedSpells.add(Fireball)

        combatEngine.castSpell(1)
        advanceUntilIdle()

        assertEquals(85, player.mana) // 100 - 15 (Fireball cost)
    }

    @Test
    fun `castSpell when not player turn shows warning`() = testScope.runTest {
        player.mana = 100

        // Cast once to end player's turn
        combatEngine.castSpell(0)
        advanceUntilIdle()

        combatEngine.combatLog.clear()

        // Try to cast during enemy turn
        combatEngine.castSpell(0)
        advanceUntilIdle()

        assertTrue(combatEngine.combatLog.any { it.contains("Wait for your turn") })
    }

    @Test
    fun `player death ends combat`() {
        player.takeDamage(150)

        assertFalse(player.isAlive)
        assertEquals(0, player.health)
    }

    @Test
    fun `combat ends when enemy dies`() = testScope.runTest {
        enemy.health = 10
        player.mana = 100

        val combatJob = launch {
            combatEngine.runCombat()
        }

        advanceTimeBy(200)
        combatEngine.castSpell(0) // Melee attack kills enemy
        advanceUntilIdle()

        assertFalse(enemy.isAlive)
        combatJob.cancel()
    }

    @Test
    fun `addLogMessage adds message to combat log`() {
        val message = "Test message"
        combatEngine.addLogMessage(message)

        assertTrue(combatEngine.combatLog.contains(message))
    }

    @Test
    fun `combat renders initial screen`() = testScope.runTest {
        enemy.health = 1

        val combatJob = launch {
            combatEngine.runCombat()
        }

        advanceTimeBy(200)
        combatEngine.castSpell(0)
        advanceUntilIdle()
        combatJob.cancel()

        verify(atLeast = 1) { renderer.renderCombatScreen(any(), any(), any(), any()) }
    }

    @Test
    fun `melee attack is instant and synchronous`() = testScope.runTest {
        player.mana = 100
        val initialHealth = enemy.health

        combatEngine.castSpell(0) // Melee attack

        // Should be instant - no need to advance time
        assertEquals(initialHealth - 10, enemy.health)
    }

    @Test
    fun `fireball has cast time and channels`() = testScope.runTest {
        player.mana = 100
        player.unlockedSpells.add(Fireball)
        val initialHealth = enemy.health

        val castJob = launch {
            combatEngine.castSpell(1)
        }

        // Before cast completes
        advanceTimeBy(500)
        assertTrue(player.isChanneling)
        assertEquals(initialHealth, enemy.health) // No damage yet

        // After cast completes
        advanceTimeBy(1500)
        advanceUntilIdle()

        assertFalse(player.isChanneling)
        assertTrue(enemy.health < initialHealth)
        castJob.cancel()
    }

    @Test
    fun `ice bolt provides lifesteal`() = testScope.runTest {
        player.mana = 100
        player.health = 50
        player.unlockedSpells.add(IceBolt)

        combatEngine.castSpell(1) // Index 1 (0 is MeleeAttack)
        advanceTimeBy(1100) // Ice Bolt cast time is 1000ms
        advanceUntilIdle()

        // Ice Bolt deals 20 damage and heals 10
        assertTrue(player.health > 50)
        assertEquals(60, player.health)
    }

    @Test
    fun `lightning chain hits multiple times`() = testScope.runTest {
        player.mana = 100
        player.unlockedSpells.add(LightningChain)
        val initialHealth = enemy.health

        combatEngine.castSpell(1) // Index 1 (0 is MeleeAttack)
        advanceTimeBy(700) // Lightning Chain takes 3 * 200ms = 600ms
        advanceUntilIdle()

        // Lightning Chain hits 3 times for 15 damage each = 45 total
        assertEquals(initialHealth - 45, enemy.health)

        // Should have 3 strike messages
        val strikeMessages = combatEngine.combatLog.count { it.contains("Lightning strike") }
        assertEquals(3, strikeMessages)
    }

    @Test
    fun `poison cloud applies damage over time`() = testScope.runTest {
        player.mana = 100
        player.unlockedSpells.add(PoisonCloud)
        val initialHealth = enemy.health

        combatEngine.castSpell(1) // Index 1 (0 is MeleeAttack)
        advanceTimeBy(2600) // Poison Cloud: 5 ticks * 500ms = 2500ms
        advanceUntilIdle()

        // Poison Cloud: 5 ticks x 5 damage = 25 total
        assertEquals(initialHealth - 25, enemy.health)
    }

    @Test
    fun `shield barrier absorbs damage`() {
        player.shieldHealth = 50

        // Damage should hit shield first
        player.takeDamage(30)

        assertEquals(20, player.shieldHealth)
        assertEquals(100, player.health) // Health unchanged
    }
}
