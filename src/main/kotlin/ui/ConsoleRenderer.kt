package ui

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
        println(GameColors.HEADER(UiText.MainMenu.header))
        println(UiText.MainMenu.description)
        println(GameColors.READY("  [1] Start New Game"))
        println("  [2] Continue (if save exists)")
        println("  [3] How to Play")
        println("  [Q] Quit")
        println()
        println(dim(UiText.MainMenu.prompt))
        println(GameColors.HEADER(UiText.MainMenu.footer))
        print("> ")
    }

    fun renderInitialTeaching() {
        clearScreen()
        println(GameColors.TEACHING(UiText.InitialTeaching.header))

        // Apply formatting to specific parts of the content
        val lines = UiText.InitialTeaching.content.lines()
        lines.forEach { line ->
            when {
                line.contains("✨ MELEE ATTACK") -> {
                    val parts = line.split(" - ")
                    println("  ${GameColors.READY(parts[0])} - ${parts.getOrNull(1) ?: ""}")
                }
                line.contains("💡 Key Concept") -> println("  ${GameColors.TEACHING(line.trim())}")
                line.contains("Code Example:") -> println("  ${bold(line.trim())}")
                line.contains("Limitation:") -> println("  ${GameColors.DAMAGE(line.trim())}")
                line.contains("Real-world analogy:") -> println("  ${bold(line.trim())}")
                line.trim().startsWith("fun ") || line.trim().startsWith("target.") ||
                line.trim().startsWith("attack(") || line.trim().startsWith("}") ||
                line.trim().startsWith("// Total") -> println(dim(line))
                else -> println(line)
            }
        }

        println(GameColors.TEACHING(UiText.InitialTeaching.footer))
        print(UiText.InitialTeaching.prompt)
    }

    fun renderHowToPlay() {
        clearScreen()
        println(GameColors.HEADER(UiText.HowToPlay.header))

        // Apply formatting to specific parts of the content
        val lines = UiText.HowToPlay.content.lines()
        lines.forEach { line ->
            when {
                line.contains(":") && line.trim().matches(Regex("^[A-Z][A-Z ]+:$")) ->
                    println("  ${bold(line.trim())}")
                line.contains("parallel") -> {
                    val updated = line.replace("parallel", GameColors.TEACHING("parallel"))
                    println(updated)
                }
                line.contains("one-by-one") -> {
                    val updated = line.replace("one-by-one", GameColors.DAMAGE("one-by-one"))
                    println(updated)
                }
                line.contains("cast") -> {
                    val updated = line.replace("cast", bold("cast"))
                    println(updated)
                }
                else -> println(line)
            }
        }

        println(GameColors.HEADER(UiText.HowToPlay.footer))
        print(UiText.HowToPlay.prompt)
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
        println(GameColors.HEADER(UiText.Combat.headerPrefix + round))
        println(GameColors.HEADER("═".repeat(65)))
        println()

        // Enemy status
        renderHealthBar("ENEMY: ${enemy.name}", enemy.health, enemy.maxHealth, GameColors.ENEMY_HP)
        println()
        println(UiText.Combat.enemySprite)
        println()

        // Player status
        renderHealthBar("PLAYER: ${player.name}", player.health, player.maxHealth, GameColors.PLAYER_HP)
        renderManaBar(player.mana, player.maxMana)

        // Show shield if active
        if (player.shieldHealth > 0) {
            println(" ".repeat(40) + UiText.Combat.shieldPrefix + GameColors.SHIELD(player.shieldHealth.toString()))
        }

        // Show channeling status
        if (player.isChanneling) {
            println(" ".repeat(40) + GameColors.CASTING(UiText.Combat.channelingStatus))
        }

        println()

        // Action Points & Queue
        if (actionQueue != null) {
            println(GameColors.BORDER("═".repeat(65)))
            val apUsed = actionQueue.actionPointsUsed
            val apMax = actionQueue.maxActionPoints
            val apRemaining = actionQueue.actionPointsRemaining
            val apBar = "█".repeat(apUsed) + "░".repeat(apRemaining)
            println(GameColors.BORDER(UiText.Combat.actionPointsLabel) + GameColors.TEACHING(apBar) + " $apUsed/$apMax")

            if (!actionQueue.isEmpty) {
                println(GameColors.READY(UiText.Combat.queuedLabel + actionQueue.getActionSummary()))
                println(UiText.Combat.queuedNote)
            } else {
                println(UiText.Combat.spellInputPrompt)
            }
        }

        // Spell bar
        println(GameColors.BORDER("═".repeat(65)))
        println(GameColors.BORDER(UiText.Combat.spellBarLabel))
        renderSpellBar(player.unlockedSpells, player, actionQueue)

        // Combat log (last 10 messages to show execution details)
        println(GameColors.BORDER("═".repeat(65)))
        println(GameColors.BORDER(UiText.Combat.combatLogLabel))
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
            is Shockwave -> dim("[synchronous]")
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
        println(GameColors.READY(UiText.Victory.header))
        println()
        println(UiText.Victory.defeatedMessage(enemy.name))
        println(UiText.Victory.gainedExpMessage(enemy.expReward))
        println()
        println(UiText.Victory.currentLevelMessage(player.level))
        println(UiText.Victory.experienceMessage(player.experience, player.nextLevelExp))
        println()
        println(GameColors.READY(UiText.Victory.footer))
        print(UiText.Victory.prompt)
    }

    fun renderLevelUp(player: Player, newSpell: Spell?) {
        clearScreen()
        println(GameColors.TEACHING(UiText.SpellUnlock.header))
        println()
        println(UiText.SpellUnlock.congratulationsMessage(player.level))
        println()

        if (newSpell != null) {
            renderSpellUnlock(newSpell)
        }

        println(GameColors.TEACHING(UiText.SpellUnlock.footer))
        print(UiText.SpellUnlock.prompt)
    }

    private fun renderSpellUnlock(spell: Spell) {
        println(GameColors.READY(UiText.SpellUnlock.newSpellMessage(spell.name)))
        println()
        println("  ${spell.description}")
        println()

        // Get the teaching content based on spell type
        val teachingText = when (spell) {
            is Shockwave -> UiText.SpellUnlock.MeleeAttack.teaching
            is Fireball -> UiText.SpellUnlock.Fireball.teaching
            is IceBolt -> UiText.SpellUnlock.IceBolt.teaching
            is LightningChain -> UiText.SpellUnlock.LightningChain.teaching
            is PoisonCloud -> UiText.SpellUnlock.PoisonCloud.teaching
            is ShieldBarrier -> UiText.SpellUnlock.ShieldBarrier.teaching
            is TimeWarp -> UiText.SpellUnlock.TimeWarp.teaching
            is ChannelBlitz -> UiText.SpellUnlock.ChannelBlitz.teaching
            else -> ""
        }

        // Apply formatting to the teaching content
        teachingText.lines().forEach { line ->
            when {
                line.contains("💡 Key Concept") || line.contains("💡 Synchronous") ->
                    println("  ${GameColors.TEACHING(line.trim())}")
                line.matches(Regex("^\\s*(Code Example:|vs\\. .*:|with .*:)\\s*$")) ->
                    println("  ${bold(line.trim())}")
                line.trim().startsWith("scope.") || line.trim().startsWith("launch") ||
                line.trim().startsWith("val ") || line.trim().startsWith("delay") ||
                line.trim().startsWith("target.") || line.trim().startsWith("player.") ||
                line.trim().startsWith("}") || line.trim().startsWith("attack()") ||
                line.trim().startsWith("async") || line.trim().startsWith("repeat") ||
                line.trim().startsWith("emit") || line.trim().startsWith("for ") ||
                line.trim().startsWith("damage") || line.trim().startsWith("channel") ||
                line.trim().startsWith("Flow:") || line.trim().startsWith("Channel:") ||
                line.trim().startsWith("withContext") || line.trim().startsWith("withTimeout") ||
                line.trim().startsWith("try {") || line.trim().startsWith("catch") ||
                line.trim().startsWith("while") || line.trim().startsWith("var ") ||
                line.trim().startsWith("// ") ->
                    println(dim(line))
                else -> println(line)
            }
        }
        println()
    }

    fun renderDefeat(player: Player) {
        clearScreen()
        println(GameColors.DAMAGE(UiText.Defeat.header))

        // Apply formatting to specific parts of the content
        val lines = UiText.Defeat.content.lines()
        lines.forEach { line ->
            when {
                line.contains("ROGUE-LITE MECHANICS:") -> println(GameColors.TEACHING("  ${bold(line.trim())}"))
                line.contains("BUT you keep all") -> {
                    val message = UiText.Defeat.spellsKeptMessage(player.unlockedSpells.size)
                    println("  • ${GameColors.READY(message)}")
                }
                else -> println(line)
            }
        }

        println(GameColors.DAMAGE(UiText.Defeat.footer))
        print(UiText.Defeat.prompt)
    }

    fun renderMessage(message: String) {
        println()
        println(message)
        println()
    }
}
