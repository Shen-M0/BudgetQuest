package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_tag_table")
data class SubscriptionTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val isVisible: Boolean = true
)