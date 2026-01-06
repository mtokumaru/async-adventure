package ui

import combat.CombatEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import models.Player

class InputHandler(
    private val scope: CoroutineScope
) {
    private var combatEngine: CombatEngine? = null
    private var inputCallback: ((String) -> Unit)? = null

    fun setCombatEngine(engine: CombatEngine?) {
        this.combatEngine = engine
    }

    fun setInputCallback(callback: (String) -> Unit) {
        this.inputCallback = callback
    }

    fun handleInput(input: String) {
        val trimmed = input.trim().lowercase()

        when {
            trimmed == "q" || trimmed == "quit" -> {
                inputCallback?.invoke("quit")
            }
            trimmed == "?" || trimmed == "help" -> {
                inputCallback?.invoke("help")
            }
            trimmed == "x" || trimmed == "execute" -> {
                // Execute queued actions
                combatEngine?.let { engine ->
                    scope.launch {
                        engine.executeQueuedActions()
                    }
                }
            }
            trimmed == "c" || trimmed == "clear" -> {
                // Clear action queue
                combatEngine?.actionQueue?.clear()
                combatEngine?.addLogMessage("🗑️  Action queue cleared")
            }
            trimmed == "u" || trimmed == "undo" -> {
                // Remove last queued action
                combatEngine?.let { engine ->
                    val removed = engine.actionQueue.removeLastAction()
                    if (removed != null) {
                        engine.addLogMessage("↩️  Removed ${removed.spell.name} from queue")
                    } else {
                        engine.addLogMessage("⚠️ Nothing to undo")
                    }
                }
            }
            trimmed.matches(Regex("^\\d+(,\\d+)*,?$")) -> {
                // Queue spell number (1-8)
                val spellIndex = trimmed.toIntOrNull()?.minus(1)
                if (spellIndex != null) {
                    combatEngine?.let { engine ->
                        if (engine.queueAction(spellIndex)) {
                            val spell = engine.player.unlockedSpells[spellIndex]
                            engine.addLogMessage("✨ Casting: ${spell.name} (${spell.actionPointCost} AP)")

                            // Auto-execute the queued action immediately
                            scope.launch {
                                engine.executeQueuedActions()
                            }
                        }
                    }
                }
            }
            trimmed.isEmpty() -> {
                // Enter key - execute queued actions if any
                combatEngine?.let { engine ->
                    scope.launch {
                        if (!engine.actionQueue.isEmpty) {
                            engine.executeQueuedActions()
                        }
                    }
                }
            }
            else -> {
                inputCallback?.invoke("unknown")
            }
        }
    }
}
