package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// Helper untuk memperbarui semua widget secara instan
object WidgetUpdater {
    fun updateAllWidgets(context: Context) {
        val intent2x1 = Intent(context, MoneyManagerWidget2x1::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids2x1 = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, MoneyManagerWidget2x1::class.java)
        )
        intent2x1.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids2x1)
        context.sendBroadcast(intent2x1)

        val intent4x2 = Intent(context, MoneyManagerWidget4x2::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids4x2 = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, MoneyManagerWidget4x2::class.java)
        )
        intent4x2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids4x2)
        context.sendBroadcast(intent4x2)
    }

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return try {
            format.format(amount).replace(",00", "").replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp " + NumberFormat.getInstance().format(amount)
        }
    }

    fun getWidgetBackgroundResource(prefs: PreferencesManager): Int {
        return when (prefs.widgetBackgroundStyle) {
            PreferencesManager.WIDGET_STYLE_SOLID_DARK -> R.drawable.bg_widget_dark
            PreferencesManager.WIDGET_STYLE_SOLID_LIGHT -> R.drawable.bg_widget_light
            PreferencesManager.WIDGET_STYLE_THEME_GRADIENT -> {
                when (prefs.appTheme) {
                    PreferencesManager.THEME_ORANGE -> R.drawable.bg_widget_gradient_orange
                    PreferencesManager.THEME_BLUE -> R.drawable.bg_widget_gradient_blue
                    PreferencesManager.THEME_PURPLE -> R.drawable.bg_widget_gradient_purple
                    PreferencesManager.THEME_GREEN -> R.drawable.bg_widget_gradient_green
                    else -> R.drawable.bg_widget_gradient_teal
                }
            }
            else -> R.drawable.bg_widget_glass // Default
        }
    }

    fun getCustomBackgroundBitmap(context: Context): android.graphics.Bitmap? {
        return try {
            val file = java.io.File(context.filesDir, "widget_bg_image.jpg")
            if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun setupWidgetBackground(context: Context, views: RemoteViews, prefs: PreferencesManager) {
        if (prefs.widgetBackgroundStyle == PreferencesManager.WIDGET_STYLE_CUSTOM_IMAGE) {
            val bitmap = getCustomBackgroundBitmap(context)
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
                views.setViewVisibility(R.id.widget_bg_overlay, 0) // VISIBLE
            } else {
                val bgResId = getWidgetBackgroundResource(prefs)
                views.setImageViewResource(R.id.widget_bg_image, bgResId)
                views.setViewVisibility(R.id.widget_bg_overlay, 8) // GONE
            }
        } else {
            val bgResId = getWidgetBackgroundResource(prefs)
            views.setImageViewResource(R.id.widget_bg_image, bgResId)
            views.setViewVisibility(R.id.widget_bg_overlay, 8) // GONE
        }
    }
}

class MoneyManagerWidget2x1 : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = PreferencesManager(context)
        val database = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ambil data terbaru dari Room secara asinkron
                val transactionsList = database.transactionDao().getAllTransactions().first()
                val budget = database.transactionDao().getBudgetConfig().first()
                
                val limit = budget?.monthlyLimit ?: 5000000.0
                val totalIncome = transactionsList.filter { it.type == "INCOME" }.sumOf { it.amount }
                val totalExpense = transactionsList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val totalBalance = totalIncome - totalExpense

                val percentBudgetUsed = if (limit > 0) ((totalExpense / limit) * 100).toInt() else 0

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_layout_2x1)

                    // Atur Background Widget secara custom
                    WidgetUpdater.setupWidgetBackground(context, views, prefs)

                    // Atur Teks Utama
                    views.setTextViewText(R.id.widget_balance, WidgetUpdater.formatRupiah(totalBalance))
                    views.setTextViewText(R.id.widget_sub_text, "Anggaran: $percentBudgetUsed% terpakai")

                    // Klik widget untuk membuka aplikasi MainActivity
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 201, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, MoneyManagerWidget2x1::class.java))
            onUpdate(context, appWidgetManager, ids)
        }
    }
}

class MoneyManagerWidget4x2 : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = PreferencesManager(context)
        val database = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ambil data terbaru dari Room secara asinkron
                val transactionsList = database.transactionDao().getAllTransactions().first()
                val budget = database.transactionDao().getBudgetConfig().first()

                val limit = budget?.monthlyLimit ?: 5000000.0
                val totalIncome = transactionsList.filter { it.type == "INCOME" }.sumOf { it.amount }
                val totalExpense = transactionsList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val totalBalance = totalIncome - totalExpense

                val lastTxn: Transaction? = transactionsList.firstOrNull()

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_layout_4x2)

                    // Atur Latar belakang widget
                    WidgetUpdater.setupWidgetBackground(context, views, prefs)

                    // Bind detail nominal saldo, pemasukan, pengeluaran
                    views.setTextViewText(R.id.widget_balance, WidgetUpdater.formatRupiah(totalBalance))
                    views.setTextViewText(R.id.widget_income, "Masuk: ${WidgetUpdater.formatRupiah(totalIncome)}")
                    views.setTextViewText(R.id.widget_expense, "Keluar: ${WidgetUpdater.formatRupiah(totalExpense)}")
                    views.setTextViewText(R.id.widget_budget_indicator, "Batas Bulanan: ${WidgetUpdater.formatRupiah(limit)}")

                    // Bind detail transaksi terakhir jika ada
                    if (lastTxn != null) {
                        val isInc = lastTxn.type == "INCOME"
                        views.setTextViewText(R.id.widget_last_txn_title, lastTxn.title)
                        
                        val prefix = if (isInc) "+" else "-"
                        val formattedAmount = "$prefix${WidgetUpdater.formatRupiah(lastTxn.amount)}"
                        views.setTextViewText(R.id.widget_last_txn_amount, formattedAmount)
                        
                        val textCol = if (isInc) 0xFF81C784.toInt() else 0xFFFF8A80.toInt()
                        views.setTextColor(R.id.widget_last_txn_amount, textCol)
                    } else {
                        views.setTextViewText(R.id.widget_last_txn_title, "Belum ada transaksi")
                        views.setTextViewText(R.id.widget_last_txn_amount, "")
                    }

                    // Intent Klik untuk membuka ke aplikasi utama
                    val intent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context, 402, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, MoneyManagerWidget4x2::class.java))
            onUpdate(context, appWidgetManager, ids)
        }
    }
}
