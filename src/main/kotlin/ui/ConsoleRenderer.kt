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
        println("  An RPG where you learn some Kotlin coroutine concepts through")
        println("  gameplay mechanics. Unlock new spells that teach different")
        println("  coroutine concepts!")
        println()
        println(GameColors.READY("  [1] Start New Game"))
        println("  [2] Continue (if save exists)")
        println("  [3] How to Play")
        println("  [Q] Quit")
        println()
        println(dim("  Press Enter to start, or type a number to select"))
        println(GameColors.HEADER("╚═══════════════════════════════════════════════════════════════╝"))
        print("> ")
    }

    fun renderInitialTeaching() {
        clearScreen()
        println(GameColors.TEACHING("╔═══════════════════════════════════════════════════════════════╗"))
        println(GameColors.TEACHING("║              🎓 WELCOME TO ASYNC ADVENTURE! 🎓                 ║"))
        println(GameColors.TEACHING("╠═══════════════════════════════════════════════════════════════╣"))
        println()
        println("  You've begun your journey with your first ability:")
        println()
        println("  ${GameColors.READY("✨ MELEE ATTACK")} - Synchronous Operations")
        println()
        println("  ${GameColors.TEACHING("💡 Key Concept: Synchronous (Blocking) Execution")}")
        println("     In synchronous code, operations run one-at-a-time.")
        println("     Each operation BLOCKS the thread until it completes.")
        println()
        println("  ${bold("Code Example:")}")
        println(dim("     fun attack(target: Enemy) {"))
        println(dim("         target.takeDamage(10)  // Blocks here"))
        println(dim("     }"))
        println()
        println(dim("     attack(enemy)  // Executes first"))
        println(dim("     attack(enemy)  // Waits for first to finish"))
        println(dim("     attack(enemy)  // Waits for second to finish"))
        println(dim("     // Total time: sum of all operations"))
        println()
        println("  ${GameColors.DAMAGE("Limitation:")} Can't do multiple things at once!")
        println()
        println("  ${bold("Real-world analogy:")} Like waiting in line at a coffee shop -")
        println("  only one person can be served at a time.")
        println()
        println("  As you level up, you'll unlock spells that use Kotlin coroutines")
        println("  to overcome these limitations!")
        println()
        println(GameColors.TEACHING("╚═══════════════════════════════════════════════════════════════╝"))
        print("Press Enter to begin your adventure...")
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
        println("    • Press [1-8] to ${bold("cast")} spells (executes immediately)")
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
                println("     Actions will execute automatically")
            } else {
                println("  Type a spell number [1-8] to cast it")
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
                // MeleeAttack is now shown in initial teaching
                println("  ${GameColors.TEACHING("💡 Synchronous Operations")}")
                println("     Basic blocking execution - see initial tutorial!")
            }
            is Fireball -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: launch")}")
                println("     scope.launch { ... } starts a new coroutine that runs")
                println("     concurrently without blocking!")
                println()
                println("  ${bold("Code Example:")}")
                println(dim("     scope.launch {"))
                println(dim("         delay(1500)  // Doesn't block the thread!"))
                println(dim("         target.takeDamage(25)"))
                println(dim("     }"))
                println(dim("     // Code continues immediately"))
                println()
                println("  ${bold("vs. Synchronous:")}")
                println(dim("     attack()  // Thread blocked"))
                println(dim("     attack()  // Must wait"))
                println()
                println("  ${bold("with launch:")}")
                println(dim("     launch { fireball() }  // Returns immediately"))
                println(dim("     launch { fireball() }  // Both run concurrently!"))
                println()
                println("  Try casting multiple Fireballs - they'll run in parallel!")
            }
            is IceBolt -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: async/await")}")
                println("     async { ... } creates a coroutine that returns a value.")
                println("     Use .await() to get the result when you need it!")
                println()
                println("  ${bold("Code Example:")}")
                println(dim("     val healthGained = scope.async {"))
                println(dim("         delay(1000)"))
                println(dim("         val damage = 20"))
                println(dim("         target.takeDamage(damage)"))
                println(dim("         damage / 2  // Return healing value"))
                println(dim("     }"))
                println(dim("     val heal = healthGained.await()"))
                println(dim("     player.heal(heal)"))
                println()
                println("  ${bold("vs. launch:")}")
                println(dim("     launch { ... }  // Returns Job, no value"))
                println()
                println("  ${bold("with async:")}")
                println(dim("     async { ... }.await()  // Returns computed value"))
                println()
                println("  Ice Bolt calculates damage AND returns healing value!")
            }
            is LightningChain -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: Parallel Coroutines")}")
                println("     Launch multiple coroutines at once to run operations")
                println("     concurrently, then join() to wait for all to complete.")
                println()
                println("  ${bold("Code Example:")}")
                println(dim("     val jobs = (1..3).map { i ->"))
                println(dim("         scope.launch {"))
                println(dim("             delay(i * 200L)"))
                println(dim("             target.takeDamage(15)"))
                println(dim("         }"))
                println(dim("     }"))
                println(dim("     jobs.forEach { it.join() }  // Wait for all"))
                println()
                println("  ${bold("vs. Sequential:")}")
                println(dim("     launch { attack() }.join()  // Wait"))
                println(dim("     launch { attack() }.join()  // Then wait"))
                println(dim("     // Takes 3x as long!"))
                println()
                println("  ${bold("with Parallel:")}")
                println(dim("     All launch at once, all complete together"))
                println(dim("     // Saves time with concurrency!"))
                println()
                println("  Lightning Chain hits 3 times in parallel!")
            }
            is PoisonCloud -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: Flow")}")
                println("     Flows emit values over time like a stream.")
                println("     Use flow { emit(...) } to create, .collect { } to consume.")
                println()
                println("  ${bold("Code Example:")}")
                println(dim("     val damageFlow = flow {"))
                println(dim("         repeat(5) {"))
                println(dim("             delay(500)"))
                println(dim("             emit(5)  // Emit damage value"))
                println(dim("         }"))
                println(dim("     }"))
                println(dim("     damageFlow.collect { damage ->"))
                println(dim("         target.takeDamage(damage)"))
                println(dim("     }"))
                println()
                println("  ${bold("vs. async:")}")
                println(dim("     async { calculateOnce() }.await()  // ONE value"))
                println()
                println("  ${bold("with Flow:")}")
                println(dim("     flow { emit(1); emit(2); emit(3) }  // STREAM"))
                println(dim("     // Perfect for DoT (damage over time)!"))
                println()
                println("  Poison Cloud emits 5 damage ticks over 2.5 seconds!")
            }
            is ShieldBarrier -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: Job & Cancellation")}")
                println("     Store Job references to cancel coroutines later.")
                println("     Use withTimeout for automatic cancellation.")
                println()
                println("  ${bold("Code Example:")}")
                println(dim("     val shieldJob = scope.launch {"))
                println(dim("         try {"))
                println(dim("             withTimeout(5000) {"))
                println(dim("                 while (shield > 0) { delay(100) }"))
                println(dim("             }"))
                println(dim("         } catch (e: CancellationException) {"))
                println(dim("             cleanup()"))
                println(dim("         }"))
                println(dim("     }"))
                println(dim("     player.activeShield = shieldJob"))
                println(dim("     // Later: shieldJob.cancel()"))
                println()
                println("  ${bold("vs. Previous spells:")}")
                println(dim("     launch { ... }  // Runs to completion"))
                println()
                println("  ${bold("with Job:")}")
                println(dim("     val job = launch { ... }"))
                println(dim("     job.cancel()  // Stop it early!"))
                println()
                println("  Shield can be broken early or timeout after 5 seconds!")
            }
            is TimeWarp -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: withContext")}")
                println("     Switch to a different dispatcher for specific work.")
                println("     Use Dispatchers.Default for CPU-heavy calculations.")
                println()
                println("  ${bold("Code Example:")}")
                println(dim("     val damage = withContext(Dispatchers.Default) {"))
                println(dim("         // Heavy computation on background thread"))
                println(dim("         var total = 0"))
                println(dim("         repeat(1000000) { total += (it % 10) }"))
                println(dim("         delay(2000)"))
                println(dim("         total % 50 + 30"))
                println(dim("     }"))
                println(dim("     // Back on main thread to apply damage"))
                println(dim("     target.takeDamage(damage)"))
                println()
                println("  ${bold("vs. launch:")}")
                println(dim("     launch { ... }  // Inherits parent dispatcher"))
                println()
                println("  ${bold("with withContext:")}")
                println(dim("     withContext(Dispatchers.IO) { ... }  // Switch thread"))
                println(dim("     // Returns to original dispatcher after!"))
                println()
                println("  Time Warp runs heavy computation on background thread!")
            }
            is ChannelBlitz -> {
                println("  ${GameColors.TEACHING("💡 Key Concept: Channels")}")
                println("     Channels enable communication between coroutines.")
                println("     Use producer/consumer pattern for data streaming.")
                println()
                println("  ${bold("Code Example:")}")
                println(dim("     val channel = Channel<Int>()"))
                println()
                println(dim("     // Producer"))
                println(dim("     launch {"))
                println(dim("         repeat(10) {"))
                println(dim("             delay(200)"))
                println(dim("             channel.send(damage)"))
                println(dim("         }"))
                println(dim("         channel.close()"))
                println(dim("     }"))
                println()
                println(dim("     // Consumer"))
                println(dim("     launch {"))
                println(dim("         for (damage in channel) {"))
                println(dim("             target.takeDamage(damage)"))
                println(dim("         }"))
                println(dim("     }"))
                println()
                println("  ${bold("vs. Flow:")}")
                println(dim("     Flow: Cold - starts when collected"))
                println()
                println("  ${bold("with Channel:")}")
                println(dim("     Channel: Hot - shared between coroutines"))
                println(dim("     // Perfect for producer/consumer patterns!"))
                println()
                println("  Producer generates attacks, Consumer applies damage!")
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
