package ui

object UiText {
    object MainMenu {
        val header = """
            ╔═══════════════════════════════════════════════════════════════╗
            ║                    ASYNC ADVENTURE                            ║
            ║              Learn Coroutines Through Combat!                 ║
            ╠═══════════════════════════════════════════════════════════════╣
        """.trimIndent()

        val description = """

              An RPG where you learn some Kotlin coroutine concepts through
              gameplay mechanics. Unlock new spells that teach different
              coroutine concepts!

        """.trimIndent()

        val options = """
              [1] Start New Game
              [2] Continue (if save exists)
              [3] How to Play
              [Q] Quit
        """.trimIndent()

        val prompt = "  Press Enter to start, or type a number to select"

        val footer = "╚═══════════════════════════════════════════════════════════════╝"
    }

    object InitialTeaching {
        val header = """
            ╔═══════════════════════════════════════════════════════════════╗
            ║              🎓 WELCOME TO ASYNC ADVENTURE! 🎓                 ║
            ╠═══════════════════════════════════════════════════════════════╣
        """.trimIndent()

        val content = """

              You've begun your journey with your first ability:

              ✨ MELEE ATTACK - Synchronous Operations

              💡 Key Concept: Synchronous (Blocking) Execution
                 In synchronous code, operations run one-at-a-time.
                 Each operation BLOCKS the thread until it completes.

              Code Example:
                 fun attack(target: Enemy) {
                     target.takeDamage(10)  // Blocks here
                 }

                 attack(enemy)  // Executes first
                 attack(enemy)  // Waits for first to finish
                 attack(enemy)  // Waits for second to finish
                 // Total time: sum of all operations

              Limitation: Can't do multiple things at once!

              Real-world analogy: Like waiting in line at a coffee shop -
              only one person can be served at a time.

              As you level up, you'll unlock spells that use Kotlin coroutines
              to overcome these limitations!

        """.trimIndent()

        val footer = "╚═══════════════════════════════════════════════════════════════╝"

        val prompt = "Press Enter to begin your adventure..."
    }

    object HowToPlay {
        val header = """
            ╔═══════════════════════════════════════════════════════════════╗
            ║                      HOW TO PLAY                              ║
            ╠═══════════════════════════════════════════════════════════════╣
        """.trimIndent()

        val content = """

              OBJECTIVE:
                Defeat enemies to gain experience and unlock new spells!
                Each spell teaches a different coroutine concept.

              CONTROLS:
                • Press [1-8] to cast spells (executes immediately)
                • Type 'Q' to quit
                • Type '?' for help during combat

              ACTION POINTS (NEW!):
                • Each turn you have 5 action points (AP)
                • ⚡ Concurrent spells run in parallel (e.g., 2× Fireball)
                • 🔄 Sequential spells run one-by-one (e.g., 3× Melee)
                • Mix & match to see the difference!

              ROGUE-LITE MECHANICS:
                • When you die, you keep all unlocked spells!
                • Your level and HP reset, but your knowledge persists
                • Try different spell combinations to progress further

              LEARNING PATH:
                Level 1: Melee Attack (synchronous)
                Level 2: Fireball (launch)
                Level 3: Ice Bolt (async/await)
                Level 4: Lightning Chain (parallel launch)
                Level 5: Poison Cloud (Flow)
                Level 6: Shield Barrier (cancellation)
                Level 7: Time Warp (withContext)
                Level 8: Channel Blitz (Channels)

        """.trimIndent()

        val footer = "╚═══════════════════════════════════════════════════════════════╝"

        val prompt = "Press Enter to return..."
    }

    object Combat {
        const val headerPrefix = "  ASYNC ADVENTURE - Round "
        const val enemySprite = "       ⚔️"
        const val shieldPrefix = "🛡️  Shield: "
        const val channelingStatus = "⏳ Channeling..."
        const val actionPointsLabel = "  ACTION POINTS: "
        const val queuedLabel = "  📋 QUEUED: "
        const val queuedNote = "     Actions will execute automatically"
        const val spellInputPrompt = "  Type a spell number [1-8] to cast it"
        const val spellBarLabel = "  SPELL BAR:"
        const val combatLogLabel = "  COMBAT LOG:"

        fun getSpellStatus(cooldown: Int, hasEnoughMana: Boolean): String {
            return when {
                cooldown > 0 -> "(Cooldown: ${cooldown}s)"
                !hasEnoughMana -> "(Not enough mana)"
                else -> "(Ready)"
            }
        }
    }

    object Victory {
        val header = """
            ╔═══════════════════════════════════════════════════════════════╗
            ║                          VICTORY!                             ║
            ╠═══════════════════════════════════════════════════════════════╣
        """.trimIndent()

        val footer = "╚═══════════════════════════════════════════════════════════════╝"

        val prompt = "Press Enter to continue..."

        fun defeatedMessage(enemyName: String) = "  You defeated $enemyName!"
        fun gainedExpMessage(expReward: Int) = "  Gained $expReward experience points"
        fun currentLevelMessage(level: Int) = "  Current Level: $level"
        fun experienceMessage(exp: Int, nextLevelExp: Int) = "  Experience: $exp/$nextLevelExp"
    }

    object Defeat {
        val header = """
            ╔═══════════════════════════════════════════════════════════════╗
            ║                          DEFEAT                               ║
            ╠═══════════════════════════════════════════════════════════════╣
        """.trimIndent()

        val content = """

              You have been defeated!

              ROGUE-LITE MECHANICS:
              • Your level and stats have been reset
              • BUT you keep all unlocked spells!
              • Use your knowledge to progress further next time

        """.trimIndent()

        val footer = "╚═══════════════════════════════════════════════════════════════╝"

        val prompt = "Press Enter to continue..."

        fun spellsKeptMessage(spellCount: Int) = "BUT you keep all $spellCount unlocked spells!"
    }

    object SpellUnlock {
        val header = """
            ╔═══════════════════════════════════════════════════════════════╗
            ║                    🎓 LEVEL UP! 🎓                             ║
            ╠═══════════════════════════════════════════════════════════════╣
        """.trimIndent()

        val footer = "╚═══════════════════════════════════════════════════════════════╝"

        val prompt = "Press Enter to continue..."

        fun congratulationsMessage(level: Int) = "  Congratulations! You reached level $level!"
        fun newSpellMessage(spellName: String) = "  ✨ NEW SPELL UNLOCKED: $spellName"

        object MeleeAttack {
            val teaching = """

                  💡 Synchronous Operations
                     Basic blocking execution - see initial tutorial!

            """.trimIndent()
        }

        object Fireball {
            val teaching = """

                  💡 Key Concept: launch
                     scope.launch { ... } starts a new coroutine that runs
                     concurrently without blocking!

                  Code Example:
                     scope.launch {
                         delay(1500)  // Doesn't block the thread!
                         target.takeDamage(25)
                     }
                     // Code continues immediately

                  vs. Synchronous:
                     attack()  // Thread blocked
                     attack()  // Must wait

                  with launch:
                     launch { fireball() }  // Returns immediately
                     launch { fireball() }  // Both run concurrently!

                  Try casting multiple Fireballs - they'll run in parallel!

            """.trimIndent()
        }

        object IceBolt {
            val teaching = """

                  💡 Key Concept: async/await
                     async { ... } creates a coroutine that returns a value.
                     Use .await() to get the result when you need it!

                  Code Example:
                     val healthGained = scope.async {
                         delay(1000)
                         val damage = 20
                         target.takeDamage(damage)
                         damage / 2  // Return healing value
                     }
                     val heal = healthGained.await()
                     player.heal(heal)

                  vs. launch:
                     launch { ... }  // Returns Job, no value

                  with async:
                     async { ... }.await()  // Returns computed value

                  Ice Bolt calculates damage AND returns healing value!

            """.trimIndent()
        }

        object LightningChain {
            val teaching = """

                  💡 Key Concept: Parallel Coroutines
                     Launch multiple coroutines at once to run operations
                     concurrently, then join() to wait for all to complete.

                  Code Example:
                     val jobs = (1..3).map { i ->
                         scope.launch {
                             delay(i * 200L)
                             target.takeDamage(15)
                         }
                     }
                     jobs.forEach { it.join() }  // Wait for all

                  vs. Sequential:
                     launch { attack() }.join()  // Wait
                     launch { attack() }.join()  // Then wait
                     // Takes 3x as long!

                  with Parallel:
                     All launch at once, all complete together
                     // Saves time with concurrency!

                  Lightning Chain hits 3 times in parallel!

            """.trimIndent()
        }

        object PoisonCloud {
            val teaching = """

                  💡 Key Concept: Flow
                     Flows emit values over time like a stream.
                     Use flow { emit(...) } to create, .collect { } to consume.

                  Code Example:
                     val damageFlow = flow {
                         repeat(5) {
                             delay(500)
                             emit(5)  // Emit damage value
                         }
                     }
                     damageFlow.collect { damage ->
                         target.takeDamage(damage)
                     }

                  vs. async:
                     async { calculateOnce() }.await()  // ONE value

                  with Flow:
                     flow { emit(1); emit(2); emit(3) }  // STREAM
                     // Perfect for DoT (damage over time)!

                  Poison Cloud emits 5 damage ticks over 2.5 seconds!

            """.trimIndent()
        }

        object ShieldBarrier {
            val teaching = """

                  💡 Key Concept: Job & Cancellation
                     Store Job references to cancel coroutines later.
                     Use withTimeout for automatic cancellation.

                  Code Example:
                     val shieldJob = scope.launch {
                         try {
                             withTimeout(5000) {
                                 while (shield > 0) { delay(100) }
                             }
                         } catch (e: CancellationException) {
                             cleanup()
                         }
                     }
                     player.activeShield = shieldJob
                     // Later: shieldJob.cancel()

                  vs. Previous spells:
                     launch { ... }  // Runs to completion

                  with Job:
                     val job = launch { ... }
                     job.cancel()  // Stop it early!

                  Shield can be broken early or timeout after 5 seconds!

            """.trimIndent()
        }

        object TimeWarp {
            val teaching = """

                  💡 Key Concept: withContext
                     Switch to a different dispatcher for specific work.
                     Use Dispatchers.Default for CPU-heavy calculations.

                  Code Example:
                     val damage = withContext(Dispatchers.Default) {
                         // Heavy computation on background thread
                         var total = 0
                         repeat(1000000) { total += (it % 10) }
                         delay(2000)
                         total % 50 + 30
                     }
                     // Back on main thread to apply damage
                     target.takeDamage(damage)

                  vs. launch:
                     launch { ... }  // Inherits parent dispatcher

                  with withContext:
                     withContext(Dispatchers.IO) { ... }  // Switch thread
                     // Returns to original dispatcher after!

                  Time Warp runs heavy computation on background thread!

            """.trimIndent()
        }

        object ChannelBlitz {
            val teaching = """

                  💡 Key Concept: Channels
                     Channels enable communication between coroutines.
                     Use producer/consumer pattern for data streaming.

                  Code Example:
                     val channel = Channel<Int>()

                     // Producer
                     launch {
                         repeat(10) {
                             delay(200)
                             channel.send(damage)
                         }
                         channel.close()
                     }

                     // Consumer
                     launch {
                         for (damage in channel) {
                             target.takeDamage(damage)
                         }
                     }

                  vs. Flow:
                     Flow: Cold - starts when collected

                  with Channel:
                     Channel: Hot - shared between coroutines
                     // Perfect for producer/consumer patterns!

                  Producer generates attacks, Consumer applies damage!

            """.trimIndent()
        }
    }
}

