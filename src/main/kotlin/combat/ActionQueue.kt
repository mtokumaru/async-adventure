package combat

import models.Spell

data class QueuedAction(
    val spell: Spell,
    val actionNumber: Int  // For display purposes (e.g., "Fireball #1", "Fireball #2")
)

class ActionQueue(
    val maxActionPoints: Int = 5
) {
    private val actions = mutableListOf<QueuedAction>()
    private var nextActionNumber = 1

    val queuedActions: List<QueuedAction> get() = actions.toList()
    val actionPointsUsed: Int get() = actions.sumOf { it.spell.actionPointCost }
    val actionPointsRemaining: Int get() = maxActionPoints - actionPointsUsed
    val isEmpty: Boolean get() = actions.isEmpty()
    val size: Int get() = actions.size

    fun canAddAction(spell: Spell): Boolean {
        return actionPointsUsed + spell.actionPointCost <= maxActionPoints
    }

    fun addAction(spell: Spell): Boolean {
        if (!canAddAction(spell)) return false

        actions.add(QueuedAction(spell, nextActionNumber++))
        return true
    }

    fun removeLastAction(): QueuedAction? {
        return if (actions.isNotEmpty()) {
            actions.removeAt(actions.size - 1)
        } else {
            null
        }
    }

    fun clear() {
        actions.clear()
        nextActionNumber = 1
    }

    fun getActionSummary(): String {
        if (actions.isEmpty()) return "No actions queued"

        val grouped = actions.groupBy { it.spell.name }
        return grouped.entries.joinToString(", ") { (spellName, actions) ->
            if (actions.size == 1) {
                spellName
            } else {
                "$spellName ×${actions.size}"
            }
        }
    }
}
