package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BudgetConfig
import com.example.data.Transaction
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val budgetConfig: StateFlow<BudgetConfig> = repository.budgetConfig
        .map { it ?: BudgetConfig() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BudgetConfig()
        )

    val totalBalance: StateFlow<Double> = allTransactions
        .map { list ->
            list.sumOf { if (it.type == "INCOME") it.amount else -it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalIncome: StateFlow<Double> = allTransactions
        .map { list ->
            list.filter { it.type == "INCOME" }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalExpense: StateFlow<Double> = allTransactions
        .map { list ->
            list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val categoryExpenses: StateFlow<Map<String, Double>> = allTransactions
        .map { list ->
            list.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun addTransaction(title: String, amount: Double, type: String, category: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun updateBudgetLimit(limit: Double) {
        viewModelScope.launch {
            repository.updateBudgetConfig(BudgetConfig(monthlyLimit = limit))
        }
    }
}

class TransactionViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
