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
            trimmed.matches(Regex("\\d")) -> {
                // Spell number (1-8)
                val spellIndex = trimmed.toIntOrNull()?.minus(1)
                if (spellIndex != null) {
                    combatEngine?.let { engine ->
                        scope.launch {
                            engine.castSpell(spellIndex)
                        }
                    }
                }
            }
            trimmed.isEmpty() -> {
                // Enter key - just acknowledge
                inputCallback?.invoke("enter")
            }
            else -> {
                inputCallback?.invoke("unknown")
            }
        }
    }
}
