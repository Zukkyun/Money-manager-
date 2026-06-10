package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.ui.TransactionViewModel
import com.example.ui.TransactionViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.WidgetUpdater
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefsManager = remember { PreferencesManager(context) }
            
            var selectedTheme by remember { mutableStateOf(prefsManager.appTheme) }
            var isDarkTheme by remember { mutableStateOf(prefsManager.isDarkMode) }

            MyApplicationTheme(themeType = selectedTheme, darkTheme = isDarkTheme) {
                val database = recuerAppDatabase(context)
                val repository = remember { TransactionRepository(database.transactionDao()) }
                val factory = remember { TransactionViewModelFactory(repository) }
                val viewModel: TransactionViewModel = viewModel(factory = factory)

                MoneyManagerApp(
                    viewModel = viewModel,
                    prefsManager = prefsManager,
                    currentTheme = selectedTheme,
                    onThemeChange = { theme ->
                        prefsManager.appTheme = theme
                        selectedTheme = theme
                    },
                    currentDark = isDarkTheme,
                    onDarkChange = { dark ->
                        prefsManager.isDarkMode = dark
                        isDarkTheme = dark
                    }
                )
            }
        }
    }
}

@Composable
fun recuerAppDatabase(context: android.content.Context): AppDatabase {
    return remember { AppDatabase.getDatabase(context) }
}

// Data model untuk visualisasi kategori
data class TransactionCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun getCategories(): List<TransactionCategory> {
    return listOf(
        TransactionCategory("makanan", "Makanan", Icons.Default.Restaurant, Color(0xFF4CAF50)),
        TransactionCategory("transportasi", "Transportasi", Icons.Default.DirectionsCar, Color(0xFF2196F3)),
        TransactionCategory("belanja", "Belanja", Icons.Default.ShoppingBag, Color(0xFFFF9800)),
        TransactionCategory("hiburan", "Hiburan", Icons.Default.SportsEsports, Color(0xFF9C27B0)),
        TransactionCategory("tagihan", "Tagihan", Icons.Default.ReceiptLong, Color(0xFFE91E63)),
        TransactionCategory("gaji", "Gaji", Icons.Default.Payments, Color(0xFF009688)),
        TransactionCategory("investasi", "Investasi", Icons.Default.TrendingUp, Color(0xFF3F51B5)),
        TransactionCategory("lainnya", "Lain-lain", Icons.Default.Category, Color(0xFF795548))
    )
}

// Formater Rupiah (IDR)
fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return try {
        format.format(amount).replace(",00", "").replace("Rp", "Rp ")
    } catch (e: Exception) {
        "Rp " + NumberFormat.getInstance().format(amount)
    }
}

// Formater Tanggal
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyManagerApp(
    viewModel: TransactionViewModel,
    prefsManager: PreferencesManager,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    currentDark: Boolean,
    onDarkChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val budgetConfig by viewModel.budgetConfig.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val categoryExpenses by viewModel.categoryExpenses.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }

    // Untuk filter di tab "Riwayat Transaksi"
    var selectedFilterType by remember { mutableStateOf("SEMUA") } // "SEMUA", "INCOME", "EXPENSE"
    var selectedFilterCategory by remember { mutableStateOf("SEMUA") }

    // State widget background style
    var widgetBgStyle by remember { mutableStateOf(prefsManager.widgetBackgroundStyle) }

    // Reactive Launcher: Setiap perubahan database atau preferensi, perbarui AppWidget instan!
    LaunchedEffect(transactions, budgetConfig, currentTheme, currentDark, widgetBgStyle) {
        WidgetUpdater.updateAllWidgets(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Money Manager",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (activeTab != 2) {
                FloatingActionButton(
                    onClick = { showAddBottomSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_transaction_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Transaksi")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Header (Ringkasan vs Riwayat Transaksi vs Pengaturan)
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Ringkasan", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Transaksi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Pengaturan", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }

            when (activeTab) {
                0 -> {
                    SummaryTabContent(
                        totalBalance = totalBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        budgetConfigLimit = budgetConfig.monthlyLimit,
                        categoryExpenses = categoryExpenses,
                        onEditBudgetClick = { showEditBudgetDialog = true },
                        transactions = transactions,
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }
                1 -> {
                    TransactionsTabContent(
                        transactions = transactions,
                        selectedFilterType = selectedFilterType,
                        onFilterTypeChange = { selectedFilterType = it },
                        selectedFilterCategory = selectedFilterCategory,
                        onFilterCategoryChange = { selectedFilterCategory = it },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }
                2 -> {
                    SettingsTabContent(
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        currentDark = currentDark,
                        onDarkChange = onDarkChange,
                        widgetBgStyle = widgetBgStyle,
                        onWidgetBgStyleChange = { style ->
                            prefsManager.widgetBackgroundStyle = style
                            widgetBgStyle = style
                        },
                        totalBalance = totalBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense
                    )
                }
            }
        }

        // Add Transaction Sheet
        if (showAddBottomSheet) {
            AddTransactionSheet(
                onDismiss = { showAddBottomSheet = false },
                onSave = { title, amount, type, category ->
                    viewModel.addTransaction(title, amount, type, category, System.currentTimeMillis())
                    showAddBottomSheet = false
                }
            )
        }

        // Edit Budget Dialog
        if (showEditBudgetDialog) {
            EditBudgetDialog(
                currentLimit = budgetConfig.monthlyLimit,
                onDismiss = { showEditBudgetDialog = false },
                onSave = { newLimit ->
                    viewModel.updateBudgetLimit(newLimit)
                    showEditBudgetDialog = false
                }
            )
        }
    }
}

@Composable
fun SummaryTabContent(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    budgetConfigLimit: Double,
    categoryExpenses: Map<String, Double>,
    onEditBudgetClick: () -> Unit,
    transactions: List<Transaction>,
    onDeleteTransaction: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Main Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Saldo Utama",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatRupiah(totalBalance),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.testTag("total_balance_text")
                    )
                }
            }
        }

        // Sub Income & Expense Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF81C784)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Pemasukan",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pemasukan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatRupiah(totalIncome),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }

                // Expense Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE57373)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Pengeluaran",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pengeluaran",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFC62828)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatRupiah(totalExpense),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFC62828),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Monthly Budget Limit Display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Batas Anggaran",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Batas Anggaran Bulanan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onEditBudgetClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Ubah Anggaran",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val rawProgress = if (budgetConfigLimit > 0) (totalExpense / budgetConfigLimit) else 0.0
                    val progressValue = rawProgress.coerceIn(0.0, 1.0).toFloat()
                    val progressColor = if (rawProgress > 1.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Terpakai: ${(rawProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (rawProgress > 1.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatRupiah(totalExpense)} / ${formatRupiah(budgetConfigLimit)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Warning Alert Banner
                    if (rawProgress > 1.0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Peringatan",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pengeluaran melebihi batas anggaran Anda!",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Category Breakdown Header & Custom Horizontal Chart
        item {
            Text(
                text = "Pengeluaran Berdasarkan Kategori",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (categoryExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat pengeluaran.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            // Urutkan pengeluaran berdasarkan nominal terbesar
            val sortedCategories = categoryExpenses.toList().sortedByDescending { it.second }
            items(sortedCategories) { (catId, amount) ->
                val categoryData = getCategories().find { it.id == catId } ?: TransactionCategory(catId, catId, Icons.Default.Category, Color.Gray)
                val percent = if (totalExpense > 0) (amount / totalExpense) else 0.0

                CategoryProgressItem(
                    category = categoryData,
                    amount = amount,
                    percentage = percent
                )
            }
        }

        // Recent Transactions Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terakhir",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        val recentTransactions = transactions.take(5)
        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada transaksi. Tambahkan sekarang!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onDelete = { onDeleteTransaction(transaction.id) }
                )
            }
        }
    }
}

@Composable
fun CategoryProgressItem(
    category: TransactionCategory,
    amount: Double,
    percentage: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(category.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = category.color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupiah(amount),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${(percentage * 100).toInt()}% dari total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percentage.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = category.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun TransactionsTabContent(
    transactions: List<Transaction>,
    selectedFilterType: String,
    onFilterTypeChange: (String) -> Unit,
    selectedFilterCategory: String,
    onFilterCategoryChange: (String) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    val categories = getCategories()

    val filteredTransactions = remember(transactions, selectedFilterType, selectedFilterCategory) {
        transactions.filter { transaction ->
            val typeMatch = when (selectedFilterType) {
                "SEMUA" -> true
                "INCOME" -> transaction.type == "INCOME"
                "EXPENSE" -> transaction.type == "EXPENSE"
                else -> true
            }
            val categoryMatch = when (selectedFilterCategory) {
                "SEMUA" -> true
                else -> transaction.category == selectedFilterCategory
            }
            typeMatch && categoryMatch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Scroll Row (Pemasukan / Pengeluaran / Semua)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedFilterType == "SEMUA",
                    onClick = { onFilterTypeChange("SEMUA") },
                    label = { Text("Semua") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilterType == "INCOME",
                    onClick = { onFilterTypeChange("INCOME") },
                    label = { Text("Pemasukan") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilterType == "EXPENSE",
                    onClick = { onFilterTypeChange("EXPENSE") },
                    label = { Text("Pengeluaran") }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Category Filter Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                InputChip(
                    selected = selectedFilterCategory == "SEMUA",
                    onClick = { onFilterCategoryChange("SEMUA") },
                    label = { Text("Semua Kategori") }
                )
            }
            items(categories) { category ->
                InputChip(
                    selected = selectedFilterCategory == category.id,
                    onClick = { onFilterCategoryChange(category.id) },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tidak ada transaksi yang cocok dengan filter aktif.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onDelete = { onDeleteTransaction(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val categoryData = getCategories().find { it.id == transaction.category }
        ?: TransactionCategory(transaction.category, transaction.category, Icons.Default.Category, Color.Gray)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryData.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryData.icon,
                        contentDescription = null,
                        tint = categoryData.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info: Title, Category, Date
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = categoryData.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                        Text(
                            text = formatDate(transaction.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Amount, Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val isIncome = transaction.type == "INCOME"
                Text(
                    text = "${if (isIncome) "+" else "-"} ${formatRupiah(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Transaksi",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" atau "INCOME"
    var selectedCategoryId by remember { mutableStateOf("makanan") }

    val categories = getCategories()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tambah Transaksi Baru",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Selector Jenis Transaksi (Row Slider-like)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (transactionType == "EXPENSE") MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                        .clickable {
                            transactionType = "EXPENSE"
                            // Ubah default kategori ke 'makanan' ketika pindah pengeluaran
                            selectedCategoryId = "makanan"
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pengeluaran",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (transactionType == "EXPENSE") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (transactionType == "INCOME") Color(0xFFE8F5E9) else Color.Transparent)
                        .clickable {
                            transactionType = "INCOME"
                            // Ubah default ke 'gaji' ketika pindah pendapatan
                            selectedCategoryId = "gaji"
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pemasukan",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (transactionType == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Input Nominal
            OutlinedTextField(
                value = amountText,
                onValueChange = { text ->
                    // Hanya izinkan angka desimal/bulat saja
                    if (text.isEmpty() || text.all { it.isDigit() }) {
                        amountText = text
                    }
                },
                label = { Text("Nominal (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_amount"),
                singleLine = true,
                placeholder = { Text("Contoh: 50000") }
            )

            // Input Catatan / Judul
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Catatan / Deskripsi") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_title"),
                singleLine = true,
                placeholder = { Text("Contoh: Beli Makan Siang") }
            )

            // Kategori Selector Header
            Text(
                text = "Pilih Kategori",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )

            // List Kategori horizontal
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredCategories = if (transactionType == "INCOME") {
                    categories.filter { it.id == "gaji" || it.id == "investasi" || it.id == "lainnya" }
                } else {
                    categories.filter { it.id != "gaji" }
                }

                items(filteredCategories) { category ->
                    val isSelected = selectedCategoryId == category.id
                    val chipBg by animateColorAsState(if (isSelected) category.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    val chipFgColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBg)
                            .clickable { selectedCategoryId = category.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = chipFgColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = chipFgColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Button(
                onClick = {
                    val amountDouble = amountText.toDoubleOrNull() ?: 0.0
                    if (amountDouble > 0 && title.isNotBlank()) {
                        onSave(title.trim(), amountDouble, transactionType, selectedCategoryId)
                    }
                },
                enabled = amountText.isNotEmpty() && (amountText.toDoubleOrNull() ?: 0.0) > 0 && title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_transaction_button")
            ) {
                Text("Simpan Transaksi", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EditBudgetDialog(
    currentLimit: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var limitText by remember { mutableStateOf(currentLimit.toInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ubah Batas Anggaran",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { text ->
                        if (text.isEmpty() || text.all { it.isDigit() }) {
                            limitText = text
                        }
                    },
                    label = { Text("Batas Anggaran Bulanan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_budget_limit"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val limitDouble = limitText.toDoubleOrNull() ?: 0.0
                            if (limitDouble > 0) {
                                onSave(limitDouble)
                            }
                        },
                        enabled = limitText.isNotEmpty() && (limitText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

// ==================== TAB CONTENT: PENGATURAN & CUSTOMIZATION ====================
@Composable
fun SettingsTabContent(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    currentDark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    widgetBgStyle: String,
    onWidgetBgStyleChange: (String) -> Unit,
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var customImageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val file = remember { java.io.File(context.filesDir, "widget_bg_image.jpg") }

    LaunchedEffect(widgetBgStyle, file) {
        if (widgetBgStyle == PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE) {
            try {
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        customImageBitmap = bitmap.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val outputStream = java.io.FileOutputStream(file)
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        // force live preview update
                        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            customImageBitmap = bitmap.asImageBitmap()
                        }

                        // force widgets update
                        WidgetUpdater.updateAllWidgets(context)
                        
                        // notify change
                        onWidgetBgStyleChange(PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE)
                        
                        Toast.makeText(context, "Selesai! Latar belakang kustom widget telah disimpan.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Section: Visual App Theme
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tema & Warna Aplikasi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Toggles Mode Gelap
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Mode Gelap (Dark Mode)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = currentDark,
                            onCheckedChange = onDarkChange
                        )
                    }

                    Text(
                        text = "Pilih Palet Warna",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    // Daftar pilihan warna tema
                    val themesList = listOf(
                        Triple(PreferencesManager.THEME_TEAL, "Classic Teal", Color(0xFF009688)),
                        Triple(PreferencesManager.THEME_ORANGE, "Sunset Glow", Color(0xFFFF5722)),
                        Triple(PreferencesManager.THEME_BLUE, "Ocean Blue", Color(0xFF2196F3)),
                        Triple(PreferencesManager.THEME_PURPLE, "Cyberpunk", Color(0xFF9C27B0)),
                        Triple(PreferencesManager.THEME_GREEN, "Sage Green", Color(0xFF4CAF50))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        themesList.forEach { (themeId, name, color) ->
                            val isSelected = currentTheme == themeId
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onThemeChange(themeId) }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name.split(" ").last(), // Teks pendek (Teal, Glow, dll)
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Widget Background Setting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kustomisasi Widget Latar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = "Gaya latar belakang widget di home screen Anda:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    // Menu pilihan background widget
                    val styles = listOf(
                        Pair(PreferencesManager.WIDGET_STYLE_GLASS, "Glass Transparan"),
                        Pair(PreferencesManager.WIDGET_STYLE_THEME_GRADIENT, "Gradasi Tema"),
                        Pair(PreferencesManager.WIDGET_STYLE_SOLID_DARK, "Hitam Obsidian"),
                        Pair(PreferencesManager.WIDGET_STYLE_SOLID_LIGHT, "Putih Bersih"),
                        Pair(PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE, "Gambar Kustom dari Galeri")
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styles.forEach { (styleKey, label) ->
                            val isSelected = widgetBgStyle == styleKey
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    )
                                    .clickable { 
                                        if (styleKey == PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE) {
                                            val internalFile = java.io.File(context.filesDir, "widget_bg_image.jpg")
                                            if (!internalFile.exists()) {
                                                launcher.launch("image/*")
                                            } else {
                                                onWidgetBgStyleChange(styleKey)
                                            }
                                        } else {
                                            onWidgetBgStyleChange(styleKey)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { 
                                            if (styleKey == PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE) {
                                                val internalFile = java.io.File(context.filesDir, "widget_bg_image.jpg")
                                                if (!internalFile.exists()) {
                                                    launcher.launch("image/*")
                                                } else {
                                                    onWidgetBgStyleChange(styleKey)
                                                }
                                            } else {
                                                onWidgetBgStyleChange(styleKey)
                                            }
                                        }
                                    )
                                }

                                if (styleKey == PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE && isSelected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { launcher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Pilih / Ubah Gambar kustom", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Live Widget Previews inside the app settings! (Outstanding UI / UX Details)
        item {
            Text(
                text = "Pratinjau Widget (Live Preview)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            // Widget 2x1 App Preview
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ukuran 2x1 (Kompak)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                val previewBg = getWidgetPreviewBackground(widgetBgStyle, currentTheme)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(previewBg)
                        .border(
                            width = 1.dp,
                            color = if (widgetBgStyle == PreferencesManager.WIDGET_STYLE_GLASS) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    if (widgetBgStyle == PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE && customImageBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = customImageBitmap!!,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                        )
                    }

                    val textCol = getWidgetPreviewTextColor(widgetBgStyle)
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "SALDO UTAMA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.sp),
                            color = textCol.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatRupiah(totalBalance),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = textCol
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Anggaran: 24% terpakai",
                            style = MaterialTheme.typography.bodySmall,
                            color = textCol.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        item {
            // Widget 4x2 App Preview
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ukuran 4x2 (Dasbor Lengkap)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                val previewBg = getWidgetPreviewBackground(widgetBgStyle, currentTheme)
                val textCol = getWidgetPreviewTextColor(widgetBgStyle)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(previewBg)
                        .border(
                            width = 1.dp,
                            color = if (widgetBgStyle == PreferencesManager.WIDGET_STYLE_GLASS) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    if (widgetBgStyle == PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE && customImageBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = customImageBitmap!!,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MONEY MANAGER",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp),
                                color = textCol.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Batas Bulanan: Rp 5.000.000",
                                style = MaterialTheme.typography.labelSmall,
                                color = textCol.copy(alpha = 0.6f)
                            )
                        }

                        HorizontalDivider(color = textCol.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Saldo Utama",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textCol.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = formatRupiah(totalBalance),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = textCol
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF81C784))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Masuk: ${formatRupiah(totalIncome)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (widgetBgStyle == PreferencesManager.WIDGET_STYLE_SOLID_LIGHT) Color(0xFF2E7D32) else Color(0xFF81C784)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF8A80))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Keluar: ${formatRupiah(totalExpense)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (widgetBgStyle == PreferencesManager.WIDGET_STYLE_SOLID_LIGHT) Color(0xFFC62828) else Color(0xFFFF8A80)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = textCol.copy(alpha = 0.1f))

                        Row {
                            Text(
                                text = "Terakhir:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = textCol.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contoh Belanja Warung",
                                style = MaterialTheme.typography.labelSmall,
                                color = textCol.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "-Rp 15.000",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (widgetBgStyle == PreferencesManager.WIDGET_STYLE_SOLID_LIGHT) Color(0xFFC62828) else Color(0xFFFF8A80)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Map preference style ke warna pratinjau
@Composable
fun getWidgetPreviewBackground(style: String, appTheme: String): Color {
    return when (style) {
        PreferencesManager.WIDGET_STYLE_SOLID_DARK -> Color(0xFF1A1A1A)
        PreferencesManager.WIDGET_STYLE_SOLID_LIGHT -> Color(0xFFFFFFFF)
        PreferencesManager.WIDGET_STYLE_THEME_GRADIENT -> {
            when (appTheme) {
                PreferencesManager.THEME_ORANGE -> Color(0xFFD84315)
                PreferencesManager.THEME_BLUE -> Color(0xFF1565C0)
                PreferencesManager.THEME_PURPLE -> Color(0xFF6A1B9A)
                PreferencesManager.THEME_GREEN -> Color(0xFF2E7D32)
                else -> Color(0xFF00695C) // Teal
            }
        }
        else -> Color(0xFF1C2D2A) // Glass (Translucent Dark Slate)
    }
}

// Map preference style ke warna teks pratinjau yang cocok kontrasnya
fun getWidgetPreviewTextColor(style: String): Color {
    return when (style) {
        PreferencesManager.WIDGET_STYLE_SOLID_LIGHT -> Color(0xFF333333)
        else -> Color(0xFFFFFFFF) // Dark mode, glass, gradients look ideal with pure white contrast
    }
}
