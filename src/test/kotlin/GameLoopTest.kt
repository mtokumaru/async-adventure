import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import models.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GameLoopTest {

    private lateinit var stats: Stats
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        stats = Stats(maxHealth = 100, maxMana = 100, attack = 10, defense = 5)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `new game creates player with correct initial stats`() {
        player = Player("Hero", stats)

        assertEquals("Hero", player.name)
        assertEquals(1, player.level)
        assertEquals(0, player.experience)
        assertEquals(100, player.health)
        assertEquals(100, player.maxHealth)
        assertEquals(100, player.mana)
        assertEquals(100, player.maxMana)
        assertTrue(player.isAlive)
    }

    @Test
    fun `player starts with melee attack spell`() {
        player = Player("Hero", stats)

        assertEquals(1, player.unlockedSpells.size)
        assertEquals("Melee Attack", player.unlockedSpells[0].name)
    }

    @Test
    fun `player levels up after gaining enough experience`() {
        player = Player("Hero", stats)

        val leveledUp = player.addExperience(100)

        assertTrue(leveledUp)
        assertEquals(2, player.level)
        assertEquals(0, player.experience) // Exactly 100 exp, no carryover
        assertEquals(100, player.health) // Health restored on level up
        assertEquals(100, player.mana) // Mana restored on level up
    }

    @Test
    fun `excess experience carries over after level up`() {
        player = Player("Hero", stats)
        // Level 1 needs 100 exp to level up

        val leveledUp = player.addExperience(150)

        assertTrue(leveledUp)
        assertEquals(2, player.level)
        assertEquals(50, player.experience) // 150 - 100 = 50 carried over
        assertEquals(200, player.nextLevelExp) // Level 2 needs 200 exp total
    }

    @Test
    fun `experience accumulates correctly before level up`() {
        player = Player("Hero", stats)

        player.addExperience(30)
        assertEquals(30, player.experience)
        assertEquals(1, player.level)

        player.addExperience(40)
        assertEquals(70, player.experience)
        assertEquals(1, player.level)

        val leveledUp = player.addExperience(50) // Total: 120
        assertTrue(leveledUp)
        assertEquals(2, player.level)
        assertEquals(20, player.experience) // 120 - 100 = 20 carried over
    }

    @Test
    fun `multiple level ups in single experience gain`() {
        player = Player("Hero", stats)
        // Level 1->2 needs 100, Level 2->3 needs 200
        // Total needed: 300

        // Give 350 experience - should level up twice (1->2->3)
        val leveledUp = player.addExperience(350)

        assertTrue(leveledUp)
        assertEquals(3, player.level) // Leveled up from 1->2->3
        assertEquals(50, player.experience) // 350 - 100 - 200 = 50 remaining
        assertEquals(300, player.nextLevelExp) // Level 3 needs 300 total
    }

    @Test
    fun `experience requirement increases with each level`() {
        player = Player("Hero", stats)

        assertEquals(100, player.nextLevelExp) // Level 1

        player.addExperience(100)
        assertEquals(200, player.nextLevelExp) // Level 2

        player.addExperience(200)
        assertEquals(300, player.nextLevelExp) // Level 3

        player.addExperience(300)
        assertEquals(400, player.nextLevelExp) // Level 4
    }

    @Test
    fun `player unlocks new spell on level up`() {
        player = Player("Hero", stats)

        player.addExperience(100) // Level 2

        assertEquals(2, player.unlockedSpells.size)
        assertEquals("Melee Attack", player.unlockedSpells[0].name)
        assertEquals("Fireball", player.unlockedSpells[1].name)
    }

    @Test
    fun `player gains partial experience without leveling up`() {
        player = Player("Hero", stats)

        val leveledUp = player.addExperience(50)

        assertFalse(leveledUp)
        assertEquals(1, player.level)
        assertEquals(50, player.experience)
    }

    @Test
    fun `victory grants experience and may trigger level up`() {
        player = Player("Hero", stats)
        val enemy = Enemy("Goblin", 50, 10, 100) // 100 exp reward

        val leveledUp = player.addExperience(enemy.expReward)

        assertTrue(leveledUp)
        assertEquals(2, player.level)
    }

    @Test
    fun `defeat resets player but keeps unlocked spells`() {
        player = Player("Hero", stats)

        // Level up and unlock spells
        player.addExperience(100) // Level 2
        player.addExperience(200) // Level 3
        assertEquals(3, player.level)
        assertEquals(3, player.unlockedSpells.size)

        // Take damage
        player.health = 50
        player.mana = 30

        // Reset (rogue-lite mechanic)
        player.reset()

        // Stats reset
        assertEquals(1, player.level)
        assertEquals(0, player.experience)
        assertEquals(100, player.health)
        assertEquals(100, player.mana)

        // Spells preserved
        assertEquals(3, player.unlockedSpells.size)
        assertTrue(player.unlockedSpells.any { it.name == "Melee Attack" })
        assertTrue(player.unlockedSpells.any { it.name == "Fireball" })
        assertTrue(player.unlockedSpells.any { it.name == "Ice Bolt" })
    }

    @Test
    fun `player takes damage correctly`() {
        player = Player("Hero", stats)

        player.takeDamage(30)

        assertEquals(70, player.health)
        assertTrue(player.isAlive)
    }

    @Test
    fun `player dies when health reaches zero`() {
        player = Player("Hero", stats)

        player.takeDamage(150)

        assertEquals(0, player.health)
        assertFalse(player.isAlive)
    }

    @Test
    fun `shield absorbs damage before health`() {
        player = Player("Hero", stats)
        player.shieldHealth = 50

        player.takeDamage(30)

        assertEquals(20, player.shieldHealth)
        assertEquals(100, player.health) // Health unchanged
    }

    @Test
    fun `excess damage goes through broken shield to health`() {
        player = Player("Hero", stats)
        player.shieldHealth = 20

        player.takeDamage(50)

        assertEquals(0, player.shieldHealth)
        assertEquals(70, player.health) // 100 - (50 - 20)
    }

    @Test
    fun `player can cast spell when mana and cooldown allow`() {
        player = Player("Hero", stats)
        val spell = Shockwave

        assertTrue(player.canCastSpell(spell))
    }

    @Test
    fun `player cannot cast spell with insufficient mana`() {
        player = Player("Hero", stats)
        player.mana = 10
        player.unlockedSpells.add(Fireball) // Costs 15 mana

        assertFalse(player.canCastSpell(Fireball))
    }

    @Test
    fun `player cannot cast spell on cooldown`() {
        player = Player("Hero", stats)
        player.unlockedSpells.add(Fireball)

        // Set cooldown
        player.setCooldown(Fireball)

        assertFalse(player.canCastSpell(Fireball))
    }

    @Test
    fun `cooldown expires after cooldown period`() {
        player = Player("Hero", stats)
        player.unlockedSpells.add(Fireball)

        player.setCooldown(Fireball)
        assertFalse(player.canCastSpell(Fireball))

        // Wait for cooldown (Fireball cooldown is 3000ms)
        Thread.sleep(3100)

        assertTrue(player.canCastSpell(Fireball))
    }

    @Test
    fun `enemy is created with correct stats for level`() {
        val level1Enemy = Enemy.createForLevel(1)
        assertEquals("Goblin Scout", level1Enemy.name)
        assertEquals(50, level1Enemy.maxHealth)

        val level3Enemy = Enemy.createForLevel(3)
        assertEquals("Orc Warrior", level3Enemy.name)
        assertEquals(80, level3Enemy.maxHealth)

        val level5Enemy = Enemy.createForLevel(5)
        assertEquals("Dark Mage", level5Enemy.name)
        assertEquals(100, level5Enemy.maxHealth)

        val level8Enemy = Enemy.createForLevel(8)
        assertEquals("Ancient Dragon", level8Enemy.name)
        assertEquals(150, level8Enemy.maxHealth)
    }

    @Test
    fun `enemy attack damages player`() {
        player = Player("Hero", stats)
        val enemy = Enemy("TestEnemy", 50, 15, 50)
        val combatLog = mutableListOf<String>()

        enemy.attack(player, combatLog)

        assertEquals(85, player.health)
        assertTrue(combatLog.any { it.contains("attacks") })
    }

    @Test
    fun `enemy dies when health reaches zero`() {
        val enemy = Enemy("TestEnemy", 50, 10, 50)

        enemy.takeDamage(60)

        assertEquals(0, enemy.health)
        assertFalse(enemy.isAlive)
    }

    @Test
    fun `next level experience requirement scales with level`() {
        player = Player("Hero", stats)

        assertEquals(100, player.nextLevelExp) // Level 1

        player.addExperience(100)
        assertEquals(200, player.nextLevelExp) // Level 2

        player.addExperience(200)
        assertEquals(300, player.nextLevelExp) // Level 3
    }

    @Test
    fun `player progress can be saved and restored`() {
        player = Player("Hero", stats)

        // Level up and unlock spells
        player.addExperience(100) // Level 2
        player.addExperience(200) // Level 3

        // Save progress
        val progress = player.toProgress()

        assertEquals(3, progress.level)
        assertEquals(0, progress.experience)
        assertEquals(3, progress.unlockedSpellNames.size)

        // Create new player from progress
        val restoredPlayer = Player.fromProgress("Hero", progress, stats)

        assertEquals(3, restoredPlayer.level)
        assertEquals(3, restoredPlayer.unlockedSpells.size)
        assertTrue(restoredPlayer.unlockedSpells.any { it.name == "Ice Bolt" })
    }

    @Test
    fun `spell registry returns correct spell for level`() {
        assertEquals("Melee Attack", SpellRegistry.getSpellForLevel(1)?.name)
        assertEquals("Fireball", SpellRegistry.getSpellForLevel(2)?.name)
        assertEquals("Ice Bolt", SpellRegistry.getSpellForLevel(3)?.name)
        assertEquals("Lightning Chain", SpellRegistry.getSpellForLevel(4)?.name)
        assertEquals("Poison Cloud", SpellRegistry.getSpellForLevel(5)?.name)
        assertEquals("Shield Barrier", SpellRegistry.getSpellForLevel(6)?.name)
        assertEquals("Time Warp", SpellRegistry.getSpellForLevel(7)?.name)
        assertEquals("Channel Blitz", SpellRegistry.getSpellForLevel(8)?.name)
        assertNull(SpellRegistry.getSpellForLevel(99))
    }

    @Test
    fun `spell registry can find spell by name`() {
        assertEquals(Fireball, SpellRegistry.getSpellByName("Fireball"))
        assertEquals(IceBolt, SpellRegistry.getSpellByName("Ice Bolt"))
        assertNull(SpellRegistry.getSpellByName("NonexistentSpell"))
    }

    @Test
    fun `multiple enemies can exist with same stats`() {
        val enemy1 = Enemy.createForLevel(1)
        val enemy2 = Enemy.createForLevel(1)

        assertEquals(enemy1.name, enemy2.name)
        assertEquals(enemy1.maxHealth, enemy2.maxHealth)

        // They should be independent
        enemy1.takeDamage(20)
        assertEquals(30, enemy1.health)
        assertEquals(50, enemy2.health)
    }
}
