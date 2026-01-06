package ui

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import models.Enemy
import models.Player
import models.Spell
import models.*

class ConsoleRenderer {
    private val terminal = Terminal()

    fun clearScreen() {
        terminal.print("\u001b[H\u001b[2J")  // ANSI clear screen
        terminal.cursor.move {
            startOfLine()
            clearScreenAfterCursor()
        }
    }

    fun renderMainMenu() {
        clearScreen()
        println(GameColors.HEADER("╔═══════════════════════════════════════════════════════════════╗"))
        println(GameColors.HEADER("║                    ASYNC ADVENTURE                            ║"))
        println(GameColors.HEADER("║              Learn Coroutines Through Combat!                 ║"))
        println(GameColors.HEADER("╠═══════════════════════════════════════════════════════════════╣"))
        println()
        println("  A rogue-lite RPG where you learn Kotlin coroutines through")
        println("  gameplay mechanics. Unlock new spells that teach different")
        println("  coroutine concepts!")
        println()
        println(GameColors.READY("  [1] Start New Game"))
        println("  [2] How to Play")
        println("  [Q] Quit")
        println()
        println(GameColors.HEADER("╚═══════════════════════════════════════════════════════════════╝"))
        print("> ")
    }

    fun renderHowToPlay() {
        clearScreen()
        println(GameColors.HEADER("╔═══════════════════════════════════════════════════════════════╗"))
        println(GameColors.HEADER("║                      HOW TO PLAY                              ║"))
        println(GameColors.HEADER("╠═══════════════════════════════════════════════════════════════╣"))
        println()
        println("  ${bold("OBJECTIVE:")}")
        println("    Defeat enemies to gain experience and unlock new spells!")
        println("    Each spell teaches a different coroutine concept.")
        println()
        println("  ${bold("CONTROLS:")}")
        println("    • Press [1-8] to ${bold("queue")} spells (uses action points)")
        println("    • Press ${GameColors.READY("ENTER")} to ${bold("execute")} queued actions")
        println("    • Press 'U' to undo last queued action")
        println("    • Press 'C' to clear action queue")
        println("    • Type 'Q' to quit")
        println("    • Type '?' for help during combat")
        println()
        println("  ${bold("ACTION POINTS (NEW!):")}")
        println("    • Each turn you have 5 action points (AP)")
        println("    • ⚡ Concurrent spells run in ${GameColors.TEACHING("parallel")} (e.g., 2× Fireball)")
        println("    • 🔄 Sequential spells run ${GameColors.DAMAGE("one-by-one")} (e.g., 3× Melee)")
        println("    • Mix & match to see the difference!")
        println()
        println("  ${bold("ROGUE-LITE MECHANICS:")}")
        println("    • When you die, you keep all unlocked spells!")
        println("    • Your level and HP reset, but your knowledge persists")
        println("    • Try different spell combinations to progress further")
        println()
        println("  ${bold("LEARNING PATH:")}")
        println("    Level 1: Melee Attack (synchronous)")
        println("    Level 2: Fireball (launch)")
        println("    Level 3: Ice Bolt (async/await)")
        println("    Level 4: Lightning Chain (parallel launch)")
        println("    Level 5: Poison Cloud (Flow)")
        println("    Level 6: Shield Barrier (cancellation)")
        println("    Level 7: Time Warp (withContext)")
        println("    Level 8: Channel Blitz (Channels)")
        println()
        println(GameColors.HEADER("╚═══════════════════════════════════════════════════════════════╝"))
        print("Press Enter to return...")
    }

    fun renderCombatScreen(
        player: Player,
        enemy: Enemy,
        combatLog: List<String>,
        round: Int,
        actionQueue: combat.ActionQueue? = null
    ) {
        clearScreen()

        // Header
        println(GameColors.HEADER("═".repeat(65)))
        println(GameColors.HEADER("  ASYNC ADVENTURE - Round $round"))
        println(GameColors.HEADER("═".repeat(65)))
        println()

        // Enemy status
        renderHealthBar("ENEMY: ${enemy.name}", enemy.health, enemy.maxHealth, GameColors.ENEMY_HP)
        println()
        println("       ⚔️")  // Enemy sprite
        println()

        // Player status
        renderHealthBar("PLAYER: ${player.name}", player.health, player.maxHealth, GameColors.PLAYER_HP)
        renderManaBar(player.mana, player.maxMana)

        // Show shield if active
        if (player.shieldHealth > 0) {
            println(" ".repeat(40) + "🛡️  Shield: ${GameColors.SHIELD(player.shieldHealth.toString())}")
        }

        // Show channeling status
        if (player.isChanneling) {
            println(" ".repeat(40) + "${GameColors.CASTING("⏳ Channeling...")}")
        }

        println()

        // Action Points & Queue
        if (actionQueue != null) {
            println(GameColors.BORDER("═".repeat(65)))
            val apUsed = actionQueue.actionPointsUsed
            val apMax = actionQueue.maxActionPoints
            val apRemaining = actionQueue.actionPointsRemaining
            val apBar = "█".repeat(apUsed) + "░".repeat(apRemaining)
            println(GameColors.BORDER("  ACTION POINTS: ") + GameColors.TEACHING(apBar) + " $apUsed/$apMax")

            if (!actionQueue.isEmpty) {
                println(GameColors.READY("  📋 QUEUED: ${actionQueue.getActionSummary()}"))
                println("     Press ${GameColors.READY("ENTER")} to execute, ${GameColors.DAMAGE("U")} to undo, ${GameColors.DAMAGE("C")} to clear")
            } else {
                println("  Queue actions with [1-8], then press ${GameColors.READY("ENTER")} to execute")
            }
        }

        // Spell bar
        println(GameColors.BORDER("═".repeat(65)))
        println(GameColors.BORDER("  SPELL BAR:"))
        renderSpellBar(player.unlockedSpells, player, actionQueue)

        // Combat log (last 10 messages to show execution details)
        println(GameColors.BORDER("═".repeat(65)))
        println(GameColors.BORDER("  COMBAT LOG:"))
        combatLog.takeLast(10).forEach { msg ->
            println("  $msg")
        }
        println(GameColors.BORDER("═".repeat(65)))
        print("> ")
    }

    private fun renderHealthBar(
        label: String,
        current: Int,
        max: Int,
        color: com.github.ajalt.mordant.rendering.TextColors
    ) {
        val percentage = if (max > 0) (current.toFloat() / max * 10).toInt() else 0
        val filled = "█".repeat(percentage.coerceIn(0, 10))
        val empty = "░".repeat((10 - percentage).coerceIn(0, 10))

        println("  ${label.padEnd(40)}HP: ${color(filled + empty)} $current/$max")
    }

    private fun renderManaBar(current: Int, max: Int) {
        val percentage = if (max > 0) (current.toFloat() / max * 10).toInt() else 0
        val filled = "█".repeat(percentage.coerceIn(0, 10))
        val empty = "░".repeat((10 - percentage).coerceIn(0, 10))

        println(" ".repeat(40) + "MP: ${GameColors.MANA(filled + empty)} $current/$max")
    }

    private fun renderSpellBar(spells: List<Spell>, player: Player, actionQueue: combat.ActionQueue? = null) {
        spells.forEachIndexed { index, spell ->
            val key = index + 1
            val status = getSpellStatus(spell, player)
            val apCost = "${spell.actionPointCost}AP"
            val concurrency = if (spell.isConcurrent) "⚡" else "🔄"
            val teachingNote = getTeachingNote(spell)

            println("  [$key] ${spell.name.padEnd(18)} ${apCost.padEnd(4)} $concurrency ${status.padEnd(20)} $teachingNote")
        }
    }

    private fun getSpellStatus(spell: Spell, player: Player): String {
        // Check cooldown
        val cooldownRemaining = player.getCooldownRemaining(spell)
        if (cooldownRemaining > 0) {
            return GameColors.COOLDOWN("(Cooldown: ${cooldownRemaining}s)")
        }

        // Check mana
        if (player.mana < spell.manaCost) {
            return GameColors.DAMAGE("(Not enough mana)")
        }

        return GameColors.READY("(Ready)")
    }

    private fun getTeachingNote(spell: Spell): String {
        return when (spell) {
            is MeleeAttack -> dim("[synchronous]")
            is Fireball -> dim("[launch coroutine]")
            is IceBolt -> dim("[async/await]")
            is LightningChain -> dim("[parallel launch]")
            is PoisonCloud -> dim("[Flow DoT]")
            is ShieldBarrier -> dim("[Job + cancellation]")
            is TimeWarp -> dim("[withContext]")
            is ChannelBlitz -> dim("[Channel]")
            else -> ""
        }
    }

    fun renderVictory(player: Player, enemy: Enemy) {
        clearScreen()
        println(GameColors.READY("╔═══════════════════════════════════════════════════════════════╗"))
        println(GameColors.READY("║                          VICTORY!                             ║"))
        println(GameColors.READY("╠═══════════════════════════════════════════════════════════════╣"))
        println()
        println("  You defeated ${enemy.name}!")
        println("  Gained ${enemy.expReward} experience points")
        println()
        println("  Current Level: ${player.level}")
        println("  Experience: ${player.experience}/${player.nextLevelExp}")
        println()
        println(GameColors.READY("╚═══════════════════════════════════════════════════════════════╝"))
        print("Press Enter to continue...")
    }

    fun renderLevelUp(player: Player, newSpell: Spell?) {
        clearScreen()
        println(GameColors.TEACHING("╔═══════════════════════════════════════════════════════════════╗"))
        println(GameColors.TEACHING("║                    🎓 LEVEL UP! 🎓                             ║"))
        println(GameColors.TEACHING("╠═══════════════════════════════════════════════════════════════╣"))
        println()
        println("  Congratulations! You reached level ${player.level}!")
        println()

        if (newSpell != null) {
            renderSpellUnlock(newSpell)
        }

        println(GameColors.TEACHING("╚═══════════════════════════════════════════════════════════════╝"))
        print("Press Enter to continue...")
    }

    private fun renderSpellUnlock(spell: Spell) {
        println(GameColors.READY("  ✨ NEW SPELL UNLOCKED: ${spell.name}"))
        println()
        println("  ${spell.description}")
        println()

        // Add teaching moment based on spell
        when (spell) {
            is MeleeAttack -> {
                // No special teaching moment for basic attack
            }
            is Fireball -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: launch")}")
                println("     scope.launch { ... } starts a new coroutine that runs")
                println("     concurrently without blocking the main thread!")
            }
            is IceBolt -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: async/await")}")
                println("     async { ... } creates a coroutine that returns a value.")
                println("     Use .await() to get the result when you need it!")
            }
            is LightningChain -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: parallel coroutines")}")
                println("     Launch multiple coroutines at once to run operations")
                println("     concurrently. They'll all execute in parallel!")
            }
            is PoisonCloud -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: Flow")}")
                println("     Flows emit values over time. Use flow { emit(...) }")
                println("     to create a stream, and .collect { } to process it!")
            }
            is ShieldBarrier -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: cancellation")}")
                println("     Store Job references to cancel coroutines later.")
                println("     Use withTimeout for automatic cancellation!")
            }
            is TimeWarp -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: withContext")}")
                println("     Switch to a different dispatcher for CPU-heavy work.")
                println("     Use Dispatchers.Default for background calculations!")
            }
            is ChannelBlitz -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: Channels")}")
                println("     Channels enable communication between coroutines.")
                println("     Use producer/consumer pattern for sequential processing!")
            }
        }
        println()
    }

    fun renderDefeat(player: Player) {
        clearScreen()
        println(GameColors.DAMAGE("╔═══════════════════════════════════════════════════════════════╗"))
        println(GameColors.DAMAGE("║                          DEFEAT                               ║"))
        println(GameColors.DAMAGE("╠═══════════════════════════════════════════════════════════════╣"))
        println()
        println("  You have been defeated!")
        println()
        println(GameColors.TEACHING("  ${bold("ROGUE-LITE MECHANICS:")}"))
        println("  • Your level and stats have been reset")
        println("  • ${GameColors.READY("BUT you keep all ${player.unlockedSpells.size} unlocked spells!")}")
        println("  • Use your knowledge to progress further next time")
        println()
        println(GameColors.DAMAGE("╚═══════════════════════════════════════════════════════════════╝"))
        print("Press Enter to continue...")
    }

    fun renderMessage(message: String) {
        println()
        println(message)
        println()
    }
}
