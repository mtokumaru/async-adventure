package models

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

sealed interface Spell {
    val name: String
    val manaCost: Int
    val cooldown: Long  // milliseconds
    val castTime: Long  // milliseconds for visualization
    val description: String
    val actionPointCost: Int  // Action points required to cast
    val isConcurrent: Boolean  // Whether multiple casts can run concurrently

    suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>)
}

// 1. Melee Attack - Synchronous (no coroutines)
data object MeleeAttack : Spell {
    override val name = "Melee Attack"
    override val manaCost = 0
    override val cooldown = 0L
    override val castTime = 0L
    override val description = "Instant physical attack (synchronous)"
    override val actionPointCost = 1
    override val isConcurrent = false  // Sequential execution

    override suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>) {
        // No suspension - instant but blocks
        target.takeDamage(10)
        combatLog.add("⚔️ You strike for 10 damage!")
    }
}

// 2. Fireball - First coroutine (launch)
data object Fireball : Spell {
    override val name = "Fireball"
    override val manaCost = 15
    override val cooldown = 3000L
    override val castTime = 1500L
    override val description = "Channeled fire spell (launch)"
    override val actionPointCost = 2
    override val isConcurrent = true  // Can launch multiple concurrently!

    override suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>) {
        caster.isChanneling = true
        combatLog.add("🔥 Channeling Fireball... // scope.launch { delay(1500) }")
        delay(1500)  // 1.5s cast time
        target.takeDamage(25)
        caster.isChanneling = false
        combatLog.add("🔥 Fireball hits for 25 damage!")
    }
}

// 3. Ice Bolt - async/await (lifesteal)
data object IceBolt : Spell {
    override val name = "Ice Bolt"
    override val manaCost = 20
    override val cooldown = 4000L
    override val castTime = 1000L
    override val description = "Lifesteal spell (async/await)"
    override val actionPointCost = 2
    override val isConcurrent = true  // Can run concurrently

    override suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>) {
        val healthGained = scope.async {
            combatLog.add("❄️ Casting Ice Bolt... // async { ... }.await()")
            delay(1000)
            val damage = 20
            target.takeDamage(damage)
            damage / 2  // Return health to restore
        }

        val heal = healthGained.await()
        caster.heal(heal)
        combatLog.add("❄️ Ice Bolt deals 20 damage and restores $heal HP!")
    }
}

// 4. Lightning Chain - Multiple launches (parallel)
data object LightningChain : Spell {
    override val name = "Lightning Chain"
    override val manaCost = 25
    override val cooldown = 5000L
    override val castTime = 500L
    override val description = "Multi-target parallel attacks (multiple launch)"
    override val actionPointCost = 3
    override val isConcurrent = true  // Parallel execution

    override suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>) {
        // For simplicity, hit the same target 3 times with delays
        combatLog.add("⚡ Lightning Chain! // 3 parallel launch calls")

        val jobs = (1..3).map { i ->
            scope.launch {
                delay(i * 200L)  // Stagger the hits
                target.takeDamage(15)
                combatLog.add("⚡ Lightning strike $i: 15 damage!")
            }
        }

        // Wait for all to complete
        jobs.forEach { it.join() }
    }
}

// 5. Poison Cloud - Flow (Damage over Time)
data object PoisonCloud : Spell {
    override val name = "Poison Cloud"
    override val manaCost = 20
    override val cooldown = 5000L
    override val castTime = 2500L
    override val description = "DoT effect using Flow"
    override val actionPointCost = 2
    override val isConcurrent = true  // Flow can run concurrently

    override suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>) {
        combatLog.add("💀 Poison Cloud applied! // flow { emit(...) }.collect { }")

        // Create a Flow that emits damage over time
        val damageFlow = flow {
            repeat(5) {  // 5 ticks
                delay(500)  // 0.5 second per tick
                emit(5)  // 5 damage per tick
            }
        }

        // Collect the flow and apply damage
        damageFlow.collect { damage ->
            target.takeDamage(damage)
            combatLog.add("💀 Poison deals $damage damage!")
        }
    }
}

// 6. Shield Barrier - Cancellable coroutine
data object ShieldBarrier : Spell {
    override val name = "Shield Barrier"
    override val manaCost = 25
    override val cooldown = 8000L
    override val castTime = 500L
    override val description = "Absorbs damage until broken or timeout"
    override val actionPointCost = 2
    override val isConcurrent = true  // Job can run in background

    override suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>) {
        combatLog.add("🛡️ Shield activated! // Job + withTimeout + cancellation")
        caster.shieldHealth = 50

        val shieldJob = scope.launch {
            try {
                // Shield lasts 5 seconds (reduced from 10 for faster gameplay)
                withTimeout(5000) {
                    while (caster.shieldHealth > 0) {
                        delay(100)  // Check every 100ms
                    }
                }
                combatLog.add("🛡️ Shield broken!")
            } catch (e: TimeoutCancellationException) {
                combatLog.add("🛡️ Shield expired!")
            } catch (e: CancellationException) {
                combatLog.add("🛡️ Shield cancelled!")
            } finally {
                caster.shieldHealth = 0
            }
        }

        caster.activeShield = shieldJob
    }
}

// 7. Time Warp - withContext (Dispatcher switching)
data object TimeWarp : Spell {
    override val name = "Time Warp"
    override val manaCost = 30
    override val cooldown = 10000L
    override val castTime = 2000L
    override val description = "Complex calculation on background thread"
    override val actionPointCost = 3
    override val isConcurrent = true  // withContext switches dispatcher

    override suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>) {
        combatLog.add("⏰ Calculating... // withContext(Dispatchers.Default)")

        // Switch to background thread for "heavy computation"
        val damage = withContext(Dispatchers.Default) {
            // Simulate complex calculation
            var total = 0
            repeat(1000000) {
                total += (it % 10)
            }
            delay(2000)  // 2 second cast time
            total % 50 + 30  // 30-80 damage
        }

        // Back on main thread to apply damage
        target.takeDamage(damage)
        combatLog.add("⏰ Time Warp deals $damage damage!")
    }
}

// 8. Channel Blitz - Channels (producer-consumer)
data object ChannelBlitz : Spell {
    override val name = "Channel Blitz"
    override val manaCost = 35
    override val cooldown = 12000L
    override val castTime = 2000L
    override val description = "Fast attacks using Channel"
    override val actionPointCost = 3
    override val isConcurrent = true  // Channel runs concurrently

    override suspend fun cast(caster: Player, target: Enemy, scope: CoroutineScope, combatLog: MutableList<String>) {
        combatLog.add("⚡ Channel Blitz! // Channel producer-consumer")
        val attackChannel = Channel<Int>()

        // Producer: Generate attacks
        val producer = scope.launch {
            repeat(10) {
                delay(200)  // Attack every 200ms
                attackChannel.send(Random.nextInt(5, 15))  // 5-15 damage
            }
            attackChannel.close()
        }

        // Consumer: Apply damage
        val consumer = scope.launch {
            for (damage in attackChannel) {
                target.takeDamage(damage)
                combatLog.add("⚡ Blitz strike: $damage damage!")
            }
        }

        // Wait for both to complete
        producer.join()
        consumer.join()
    }
}

// Spell registry for lookup by name
object SpellRegistry {
    private val spells = listOf(
        MeleeAttack,
        Fireball,
        IceBolt,
        LightningChain,
        PoisonCloud,
        ShieldBarrier,
        TimeWarp,
        ChannelBlitz
    )

    fun getSpellByName(name: String): Spell? {
        return spells.find { it.name == name }
    }

    fun getStartingSpells(): List<Spell> {
        return listOf(MeleeAttack)
    }

    fun getSpellForLevel(level: Int): Spell? {
        return when (level) {
            1 -> MeleeAttack
            2 -> Fireball
            3 -> IceBolt
            4 -> LightningChain
            5 -> PoisonCloud
            6 -> ShieldBarrier
            7 -> TimeWarp
            8 -> ChannelBlitz
            else -> null
        }
    }
}
