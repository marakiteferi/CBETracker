package com.example.cbetracker

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<FinanceViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) readCBEMessages()
            }

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val transactions = viewModel.transactions

                // --- AUTO SYNC ON STARTUP ---
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                        readCBEMessages()
                    }
                }

                // --- DATE & MONTH SETUP ---
                val todayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val todayString = todayFormat.format(Date())

                val availableMonths = transactions.map { it.dateString.substring(3) }.distinct()
                    .sortedByDescending { monthYear ->
                        val parts = monthYear.split("/")
                        if (parts.size == 2) parts[1].toInt() * 100 + parts[0].toInt() else 0
                    }

                val availableDates = transactions.map { it.dateString }.distinct().toMutableList()
                if (!availableDates.contains(todayString)) availableDates.add(todayString)

                val sortedDates = availableDates.sortedByDescending { dateStr ->
                    val parts = dateStr.split("/")
                    if (parts.size == 3) parts[2].toInt() * 10000 + parts[1].toInt() * 100 + parts[0].toInt() else 0
                }

                // --- STATE VARIABLES ---
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                var selectedReportDate by remember { mutableStateOf(todayString) }
                var selectedMonth by remember { mutableStateOf("All") }

                Column(modifier = Modifier.fillMaxSize()) {
                    // --- TOP APP BAR & SYNC BUTTON ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CBE Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                                readCBEMessages()
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_SMS)
                            }
                        }) {
                            Text("Sync")
                        }
                    }

                    // --- TAB NAVIGATION ---
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                            Text("Daily", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                        Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                            Text("Monthly", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                    }

                    // --- TAB CONTENT ---
                    if (selectedTabIndex == 0) {
                        // --- DAILY VIEW ---
                        val dailyTransactions = transactions.filter { it.dateString == selectedReportDate }
                        val dailyIncome = dailyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                        val dailyExpense = dailyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select Date:", fontWeight = FontWeight.Bold)
                                DynamicDropdown(sortedDates, selectedReportDate, todayString) { selectedReportDate = it }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            SummaryCard("Daily Summary", dailyIncome, dailyExpense)

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            LazyColumn {
                                items(dailyTransactions) { TransactionCard(it) }
                            }
                        }
                    } else {
                        // --- MONTHLY VIEW ---
                        val monthlyTransactions = if (selectedMonth == "All") transactions else transactions.filter { it.dateString.endsWith(selectedMonth) }
                        val monthlyIncome = monthlyTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                        val monthlyExpense = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select Month:", fontWeight = FontWeight.Bold)
                                val monthOptions = listOf("All") + availableMonths
                                DynamicDropdown(monthOptions, selectedMonth, "") { selectedMonth = it }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            SummaryCard("Monthly Summary", monthlyIncome, monthlyExpense)

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            LazyColumn {
                                items(monthlyTransactions) { TransactionCard(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readCBEMessages() {
        val uri = Uri.parse("content://sms/inbox")

        // 1. Only request the 'body' column to save memory
        val projection = arrayOf("body")

        // 2. Tell the database exactly who the sender must be
        val selection = "address = ? OR address = ?"
        val selectionArgs = arrayOf("CBE", "223")

        // 3. Apply the filter directly in the query
        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )

        val messages = mutableListOf<String>()

        cursor?.use {
            val bodyIndex = it.getColumnIndex("body")
            while (it.moveToNext()) {
                messages.add(it.getString(bodyIndex))
            }
        }

        viewModel.loadTransactionsFromSMS(messages)
    }

    @Composable
    private fun SummaryCard(title: String, income: Double, expense: Double) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Income: ETB $income", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Text("Expense: ETB $expense", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val net = income - expense
                val netColor = if (net >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                Text("Net Balance: ETB $net", color = netColor, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    @Composable
    private fun TransactionCard(transaction: Transaction) {
        val isIncome = transaction.type == "INCOME"
        val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
        val typeText = if (isIncome) "Income" else "Expense"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(typeText, fontWeight = FontWeight.Bold, color = amountColor)
                    Text(transaction.dateString, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Name: ${transaction.person}")
                Text("Amount: ETB ${transaction.amount}", color = amountColor, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    private fun DynamicDropdown(options: List<String>, selectedOption: String, todayString: String, onOptionSelected: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.height(36.dp)) {
                val label = if (selectedOption == todayString && todayString.isNotEmpty()) "Today" else selectedOption
                Text(label, style = MaterialTheme.typography.bodySmall)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(if (option == todayString && todayString.isNotEmpty()) "Today ($option)" else option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}