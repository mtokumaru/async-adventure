import combat.CombatEngine
import kotlinx.coroutines.*
import models.*
import persistence.ProgressManager
import ui.ConsoleRenderer
import ui.InputHandler

class Game {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val renderer = ConsoleRenderer()
    private val inputHandler = InputHandler(scope)
    private val progressManager = ProgressManager()

    private var player: Player? = null
    private var isRunning = true

    suspend fun start() {
        while (isRunning) {
            renderer.renderMainMenu()
            val input = readLine()?.trim()?.lowercase()

            when (input) {
                "1" -> startNewGame()
                "2" -> continueGame()
                "3" -> showHowToPlay()
                "q", "quit" -> {
                    isRunning = false
                    renderer.renderMessage("Thanks for playing!")
                }
                else -> renderer.renderMessage("Invalid option!")
            }
        }
    }

    private suspend fun startNewGame() {
        // Create new player
        val stats = Stats(
            maxHealth = 100,
            maxMana = 100,
            attack = 10,
            defense = 5
        )

        player = Player("Hero", stats)

        // Start game loop
        gameLoop()
    }

    private suspend fun continueGame() {
        if (!progressManager.hasSaveFile()) {
            renderer.renderMessage("No save file found!")
            delay(2000)
            return
        }

        val progress = progressManager.loadProgress()
        if (progress == null) {
            renderer.renderMessage("Failed to load save file!")
            delay(2000)
            return
        }

        // Create player from progress
        val stats = Stats(
            maxHealth = 100,
            maxMana = 100,
            attack = 10,
            defense = 5
        )

        player = Player.fromProgress("Hero", progress, stats)

        renderer.renderMessage("Progress loaded! Level ${progress.level} with ${progress.unlockedSpellNames.size} spells")
        delay(2000)

        // Start game loop
        gameLoop()
    }

    private suspend fun showHowToPlay() {
        renderer.renderHowToPlay()
        readLine()
    }

    private suspend fun gameLoop() {
        val currentPlayer = player ?: return

        var keepPlaying = true

        while (keepPlaying && currentPlayer.isAlive) {
            // Create enemy for current level
            val enemy = Enemy.createForLevel(currentPlayer.level)

            // Start combat
            val victory = runCombat(currentPlayer, enemy)

            if (victory) {
                // Victory
                renderer.renderVictory(currentPlayer, enemy)
                readLine()

                // Gain experience
                val leveledUp = currentPlayer.addExperience(enemy.expReward)

                if (leveledUp) {
                    // Check if new spell was unlocked
                    val newSpell = SpellRegistry.getSpellForLevel(currentPlayer.level)
                    renderer.renderLevelUp(currentPlayer, newSpell)
                    readLine()
                }

                // Save progress
                progressManager.saveProgress(currentPlayer)

            } else {
                // Defeat
                renderer.renderDefeat(currentPlayer)
                readLine()

                // Rogue-lite: reset but keep spells
                currentPlayer.reset()
                progressManager.saveProgress(currentPlayer)

                // Return to main menu
                keepPlaying = false
            }
        }
    }

    private suspend fun runCombat(player: Player, enemy: Enemy): Boolean {
        val combatEngine = CombatEngine(player, enemy, scope, renderer)
        inputHandler.setCombatEngine(combatEngine)

        var combatActive = true
        var userQuit = false

        // Set up input callback
        inputHandler.setInputCallback { command ->
            when (command) {
                "quit" -> {
                    userQuit = true
                    combatActive = false
                }
                "help" -> {
                    combatEngine.addLogMessage("Press 1-8 to cast spells, Q to quit")
                }
            }
        }

        // Start combat in a coroutine
        val combatJob = scope.launch {
            combatEngine.runCombat()
            combatActive = false
        }

        // Input loop
        while (combatActive) {
            val input = withContext(Dispatchers.IO) {
                readLine()
            }

            if (input != null) {
                inputHandler.handleInput(input)
            }

            // Small delay to prevent tight loop
            delay(50)
        }

        // Wait for combat to finish
        combatJob.join()

        // Clean up
        inputHandler.setCombatEngine(null)

        // Return true if player won and didn't quit
        return player.isAlive && !userQuit
    }
}
