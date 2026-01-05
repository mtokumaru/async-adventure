package models

class Enemy(
    val name: String,
    val maxHealth: Int,
    val attackDamage: Int,
    val expReward: Int
) {
    var health: Int = maxHealth
    val isAlive: Boolean get() = health > 0

    fun takeDamage(amount: Int) {
        health = maxOf(0, health - amount)
    }

    fun attack(target: Player, combatLog: MutableList<String>) {
        target.takeDamage(attackDamage)
        combatLog.add("👹 $name attacks for $attackDamage damage!")
    }

    companion object {
        fun createForLevel(level: Int): Enemy {
            return when {
                level <= 2 -> Enemy(
                    name = "Goblin Scout",
                    maxHealth = 50,
                    attackDamage = 8,
                    expReward = 50
                )
                level <= 4 -> Enemy(
                    name = "Orc Warrior",
                    maxHealth = 80,
                    attackDamage = 12,
                    expReward = 80
                )
                level <= 6 -> Enemy(
                    name = "Dark Mage",
                    maxHealth = 100,
                    attackDamage = 15,
                    expReward = 120
                )
                else -> Enemy(
                    name = "Ancient Dragon",
                    maxHealth = 150,
                    attackDamage = 20,
                    expReward = 200
                )
            }
        }
    }
}
