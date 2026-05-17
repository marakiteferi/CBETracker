package com.example.cbetracker

data class Transaction(
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val person: String,
    val dateString: String,
    val timestamp: Long
)
