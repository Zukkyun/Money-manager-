package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_config")
data class BudgetConfig(
    @PrimaryKey val id: Int = 1,
    val monthlyLimit: Double = 5000000.0 // Default Rp 5.000.000
)
