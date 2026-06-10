package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val budgetConfig: Flow<BudgetConfig?> = transactionDao.getBudgetConfig()

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun updateBudgetConfig(config: BudgetConfig) {
        transactionDao.insertBudgetConfig(config)
    }
}
