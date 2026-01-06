package models

import kotlinx.coroutines.Job

class Player(
    val name: String,
    initialStats: Stats
) {
    var level: Int = 1
    var experience: Int = 0
    val nextLevelExp: Int get() = level * 100

    var health: Int = initialStats.maxHealth
    val maxHealth: Int = initialStats.maxHealth

    var mana: Int = initialStats.maxMana
    val maxMana: Int = initialStats.maxMana

    var shieldHealth: Int = 0
    var activeShield: Job? = null

    var isChanneling: Boolean = false

    var hasSeenInitialTeaching: Boolean = false

    val unlockedSpells: MutableList<Spell> = mutableListOf()
    private val spellCooldowns: MutableMap<Spell, Long> = mutableMapOf()

    val isAlive: Boolean get() = health > 0

    init {
        // Start with Melee Attack
        unlockedSpells.add(MeleeAttack)
    }

    fun takeDamage(amount: Int) {
        if (shieldHealth > 0) {
            // Shield absorbs damage first
            val damageToShield = minOf(amount, shieldHealth)
            shieldHealth -= damageToShield
            val remainingDamage = amount - damageToShield

            if (shieldHealth <= 0) {
                activeShield?.cancel()
                activeShield = null
            }

            if (remainingDamage > 0) {
                health = maxOf(0, health - remainingDamage)
            }
        } else {
            health = maxOf(0, health - amount)
        }
    }

    fun heal(amount: Int) {
        health = minOf(maxHealth, health + amount)
    }

    fun restoreMana(amount: Int) {
        mana = minOf(maxMana, mana + amount)
    }

    fun getCooldownRemaining(spell: Spell): Long {
        val lastCast = spellCooldowns[spell] ?: 0L
        val now = System.currentTimeMillis()
        val elapsed = now - lastCast
        val remaining = spell.cooldown - elapsed
        return if (remaining > 0) (remaining / 1000) else 0  // Return seconds
    }

    fun setCooldown(spell: Spell) {
        spellCooldowns[spell] = System.currentTimeMillis()
    }

    fun canCastSpell(spell: Spell): Boolean {
        // Check mana
        if (mana < spell.manaCost) return false

        // Check cooldown
        val cooldownRemaining = getCooldownRemaining(spell)
        if (cooldownRemaining > 0) return false

        return true
    }

    fun addExperience(amount: Int): Boolean {
        experience += amount
        var didLevelUp = false

        // Handle multiple level ups if player gains enough experience
        while (experience >= nextLevelExp) {
            levelUp()
            didLevelUp = true
        }

        return didLevelUp
    }

    private fun levelUp() {
        val oldLevelExp = nextLevelExp  // Save old level requirement before incrementing
        level++
        experience -= oldLevelExp  // Carry over excess experience

        // Restore health and mana on level up
        health = maxHealth
        mana = maxMana

        // Unlock new spell
        val newSpell = SpellRegistry.getSpellForLevel(level)
        if (newSpell != null && !unlockedSpells.contains(newSpell)) {
            unlockedSpells.add(newSpell)
        }
    }

    fun reset() {
        // Rogue-lite: Keep spells, reset stats
        level = 1
        experience = 0
        health = maxHealth
        mana = maxMana
        shieldHealth = 0
        activeShield?.cancel()
        activeShield = null
        isChanneling = false
    }

    fun toProgress(): PlayerProgress {
        return PlayerProgress(
            level = level,
            experience = experience,
            unlockedSpellNames = unlockedSpells.map { it.name },
            hasSeenInitialTeaching = hasSeenInitialTeaching
        )
    }

    companion object {
        fun fromProgress(name: String, progress: PlayerProgress, stats: Stats): Player {
            val player = Player(name, stats)
            player.level = progress.level
            player.experience = progress.experience
            player.hasSeenInitialTeaching = progress.hasSeenInitialTeaching

            // Restore unlocked spells
            player.unlockedSpells.clear()
            progress.unlockedSpellNames.forEach { spellName ->
                val spell = SpellRegistry.getSpellByName(spellName)
                if (spell != null) {
                    player.unlockedSpells.add(spell)
                }
            }

            return player
        }
    }
}
