package com.example.cbetracker

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class FinanceViewModel : ViewModel() {

    // State is now safely held in the ViewModel
    val transactions = mutableStateListOf<Transaction>()

    // We can compute these on the fly in the UI, or hold them as state.
    // For now, let's keep it simple and filter directly from the list.

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun loadTransactionsFromSMS(messages: List<String>) {
        transactions.clear()

        val newTransactions = mutableListOf<Transaction>()

        for (message in messages) {
            if (message.contains("CBE")) {
                if (message.contains("Credited with ETB")) {
                    parseIncome(message)?.let { newTransactions.add(it) }
                }
                if (message.contains("transfered ETB")) {
                    parseExpense(message)?.let { newTransactions.add(it) }
                }
            }
        }

        // Sort by newest first using the timestamp
        newTransactions.sortByDescending { it.timestamp }
        transactions.addAll(newTransactions)
    }

    private fun parseIncome(message: String): Transaction? {
        return try {
            val amountRegex = Regex("""Credited with ETB\s([\d,]+\.\d{2})""")
            val nameRegex = Regex("""from\s(.*?),\son""")
            val dateRegex = Regex("""on\s(\d{2}/\d{2}/\d{4})""")

            val amount = amountRegex.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val name = nameRegex.find(message)?.groupValues?.get(1) ?: "Unknown"
            val dateString = dateRegex.find(message)?.groupValues?.get(1) ?: "Unknown"

            val timestamp = dateFormat.parse(dateString)?.time ?: 0L

            Transaction("INCOME", amount, name, dateString, timestamp)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseExpense(message: String): Transaction? {
        return try {
            val amountRegex = Regex("""transfered ETB\s([\d,]+\.\d{2})""")
            val nameRegex = Regex("""to\s(.*?)\son""")
            val dateRegex = Regex("""on\s(\d{2}/\d{2}/\d{4})""")

            val amount = amountRegex.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val name = nameRegex.find(message)?.groupValues?.get(1) ?: "Unknown"
            val dateString = dateRegex.find(message)?.groupValues?.get(1) ?: "Unknown"

            val timestamp = dateFormat.parse(dateString)?.time ?: 0L

            Transaction("EXPENSE", amount, name, dateString, timestamp)
        } catch (e: Exception) {
            null
        }
    }
}