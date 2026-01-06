package models

import kotlinx.serialization.Serializable

@Serializable
data class Stats(
    val maxHealth: Int,
    val maxMana: Int,
    val attack: Int,
    val defense: Int
)
