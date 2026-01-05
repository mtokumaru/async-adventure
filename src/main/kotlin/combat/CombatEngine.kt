package combat

import kotlinx.coroutines.*
import models.Enemy
import models.Player
import models.Spell
import ui.ConsoleRenderer

class CombatEngine(
    val player: Player,
    val enemy: Enemy,
    private val scope: CoroutineScope,
    private val renderer: ConsoleRenderer
) {
    val combatLog: MutableList<String> = mutableListOf()
    var round: Int = 1
    private var isPlayerTurn: Boolean = true

    suspend fun runCombat(): Boolean {
        combatLog.add("Combat begins! Fight!")

        // Render initial state
        renderer.renderCombatScreen(player, enemy, combatLog, round)

        while (player.isAlive && enemy.isAlive) {
            if (isPlayerTurn) {
                // Player's turn - wait for input
                // Input will be handled by InputHandler which calls castSpell()
                // Don't render continuously - only render after actions
                delay(100)  // Small delay to prevent tight loop
            } else {
                // Enemy's turn
                delay(1000)  // Give player time to see what happened
                enemyTurn()
                isPlayerTurn = true
                round++
            }
        }

        // Combat ended
        return player.isAlive
    }

    suspend fun castSpell(spellIndex: Int) {
        if (!isPlayerTurn) {
            combatLog.add("⚠️ Wait for your turn!")
            return
        }

        if (spellIndex < 0 || spellIndex >= player.unlockedSpells.size) {
            combatLog.add("⚠️ Invalid spell!")
            return
        }

        val spell = player.unlockedSpells[spellIndex]

        // Check if player can cast the spell
        if (!player.canCastSpell(spell)) {
            if (player.mana < spell.manaCost) {
                combatLog.add("⚠️ Not enough mana! Need ${spell.manaCost}, have ${player.mana}")
            } else {
                combatLog.add("⚠️ Spell on cooldown!")
            }
            return
        }

        // Cast the spell
        try {
            player.mana -= spell.manaCost
            player.setCooldown(spell)

            // Cast spell (this is a suspend function)
            spell.cast(player, enemy, scope, combatLog)

            // Render after spell cast
            renderer.renderCombatScreen(player, enemy, combatLog, round)

            // Check if enemy died
            if (!enemy.isAlive) {
                return
            }

            // End player turn
            isPlayerTurn = false

        } catch (e: Exception) {
            combatLog.add("⚠️ Error casting spell: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun enemyTurn() {
        if (!enemy.isAlive) return

        delay(500)  // Brief pause before enemy attacks
        enemy.attack(player, combatLog)

        // Render after enemy attack
        renderer.renderCombatScreen(player, enemy, combatLog, round)
    }

    fun addLogMessage(message: String) {
        combatLog.add(message)
    }
}
