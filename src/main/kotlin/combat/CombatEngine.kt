package combat

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import models.Enemy
import models.Player
import models.Spell
import ui.ConsoleRenderer
import kotlin.system.measureTimeMillis

class CombatEngine(
    val player: Player,
    val enemy: Enemy,
    private val scope: CoroutineScope,
    private val renderer: ConsoleRenderer
) {
    val combatLog: MutableList<String> = mutableListOf()
    var round: Int = 1
    private var isPlayerTurn: Boolean = true
    val actionQueue = ActionQueue(maxActionPoints = 5)
    private val playerTurnComplete = Channel<Unit>(Channel.CONFLATED)

    suspend fun runCombat(): Boolean {
        combatLog.add("Combat begins! Fight!")
        combatLog.add("💡 NEW: Queue multiple actions per turn with action points!")
        combatLog.add("   ⚡ = Concurrent (runs in parallel) | 🔄 = Sequential (runs one-by-one)")

        // Render initial state
        renderer.renderCombatScreen(player, enemy, combatLog, round, actionQueue)

        while (player.isAlive && enemy.isAlive) {
            if (isPlayerTurn) {
                // Player's turn - wait for player to execute actions
                // This suspends until executeQueuedActions() is called
                playerTurnComplete.receive()
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
            renderer.renderCombatScreen(player, enemy, combatLog, round, actionQueue)

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
        renderer.renderCombatScreen(player, enemy, combatLog, round, actionQueue)
    }

    fun addLogMessage(message: String) {
        combatLog.add(message)
    }

    fun queueAction(spellIndex: Int): Boolean {
        if (spellIndex < 0 || spellIndex >= player.unlockedSpells.size) {
            combatLog.add("⚠️ Invalid spell!")
            return false
        }

        val spell = player.unlockedSpells[spellIndex]

        // Check if player can cast the spell
        if (!player.canCastSpell(spell)) {
            if (player.mana < spell.manaCost) {
                combatLog.add("⚠️ Not enough mana! Need ${spell.manaCost}, have ${player.mana}")
            } else {
                combatLog.add("⚠️ Spell on cooldown!")
            }
            return false
        }

        // Check action points
        if (!actionQueue.canAddAction(spell)) {
            combatLog.add("⚠️ Not enough action points! Need ${spell.actionPointCost}, have ${actionQueue.actionPointsRemaining}")
            return false
        }

        // Queue the action
        if (actionQueue.addAction(spell)) {
            // Don't deduct mana/cooldown yet - that happens on execution
            return true
        }

        return false
    }

    suspend fun executeQueuedActions() {
        if (actionQueue.isEmpty) return

        val actions = actionQueue.queuedActions
        combatLog.add("═".repeat(60))
        combatLog.add("⏱️  EXECUTING TURN: ${actionQueue.getActionSummary()}")
        combatLog.add("   Action Points Used: ${actionQueue.actionPointsUsed}/${actionQueue.maxActionPoints}")
        combatLog.add("═".repeat(60))

        // Group by concurrency
        val concurrentActions = actions.filter { it.spell.isConcurrent }
        val sequentialActions = actions.filter { !it.spell.isConcurrent }

        var totalTimeMs = 0L

        // Execute sequential actions first (blocking, one by one)
        if (sequentialActions.isNotEmpty()) {
            combatLog.add("🔄 SEQUENTIAL EXECUTION (synchronous):")
            val seqTime = measureTimeMillis {
                for (action in sequentialActions) {
                    val actionTime = measureTimeMillis {
                        combatLog.add("  ⏱️ [${String.format("%.1f", totalTimeMs / 1000.0)}s] ${action.spell.name} #${action.actionNumber} starts...")
                        executeAction(action)
                    }
                    totalTimeMs += actionTime
                    combatLog.add("  ✓ [${String.format("%.1f", totalTimeMs / 1000.0)}s] ${action.spell.name} #${action.actionNumber} complete!")
                }
            }
            combatLog.add("  📊 Sequential total: ${String.format("%.1fs", seqTime / 1000.0)}")
        }

        // Execute concurrent actions (parallel with launch)
        if (concurrentActions.isNotEmpty()) {
            combatLog.add("⚡ CONCURRENT EXECUTION (coroutines with launch):")
            val concTime = measureTimeMillis {
                val jobs = concurrentActions.map { action ->
                    scope.async {
                        val startTime = System.currentTimeMillis()
                        combatLog.add("  ⏱️ [${String.format("%.1f", totalTimeMs / 1000.0)}s] ${action.spell.name} #${action.actionNumber} launching...")
                        executeAction(action)
                        val elapsed = System.currentTimeMillis() - startTime
                        combatLog.add("  ✓ [${String.format("%.1f", (totalTimeMs + elapsed) / 1000.0)}s] ${action.spell.name} #${action.actionNumber} complete!")
                    }
                }
                jobs.awaitAll()
            }
            totalTimeMs += concTime
            combatLog.add("  📊 Concurrent total: ${String.format("%.1fs", concTime / 1000.0)} for ${concurrentActions.size} actions!")

            // Show the benefit
            val wouldHaveBeenSequential = concurrentActions.sumOf { it.spell.castTime }
            val saved = wouldHaveBeenSequential - concTime
            if (saved > 100) {  // More than 100ms saved
                combatLog.add("  💡 Concurrency saved ${String.format("%.1fs", saved / 1000.0)}! (vs ${String.format("%.1fs", wouldHaveBeenSequential / 1000.0)} sequential)")
            }
        }

        combatLog.add("═".repeat(60))
        combatLog.add("⏱️  TURN COMPLETE: Total time ${String.format("%.1fs", totalTimeMs / 1000.0)}")
        combatLog.add("═".repeat(60))

        // Render after all actions
        renderer.renderCombatScreen(player, enemy, combatLog, round, actionQueue)

        // Clear queue for next turn
        actionQueue.clear()

        // Check if enemy died
        if (!enemy.isAlive) {
            return
        }

        // End player turn and signal combat loop to continue
        isPlayerTurn = false
        playerTurnComplete.trySend(Unit)
    }

    private suspend fun executeAction(action: QueuedAction) {
        val spell = action.spell

        // Deduct mana and set cooldown
        player.mana -= spell.manaCost
        player.setCooldown(spell)

        // Cast the spell
        spell.cast(player, enemy, scope, combatLog)
    }
}
