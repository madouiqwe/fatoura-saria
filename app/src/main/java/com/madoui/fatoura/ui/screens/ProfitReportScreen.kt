package com.madoui.fatoura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.ui.components.EmptyState
import com.madoui.fatoura.ui.components.FatouraTopBar
import com.madoui.fatoura.ui.theme.Background
import com.madoui.fatoura.ui.theme.Green
import com.madoui.fatoura.ui.theme.GreenSoft
import com.madoui.fatoura.ui.theme.LineColor
import com.madoui.fatoura.ui.theme.Teal
import com.madoui.fatoura.ui.theme.TealSoft
import com.madoui.fatoura.ui.theme.TextMuted
import com.madoui.fatoura.ui.theme.TextPrimary
import com.madoui.fatoura.util.Fmt
import java.util.Calendar

/** نطاق التقرير الزمني. */
private enum class ProfitRange(val label: String) {
    TODAY("اليوم"),
    WEEK("آخر 7 أيام"),
    MONTH("آخر 30 يوم"),
    ALL("كل الفواتير")
}

/**
 * تقرير الأرباح: يقارن المبيعات بتكلفة البضاعة المباعة، ويحتاج
 * تسجيل «سعر شراء الحبة» في دليل السلع لتفعيل الحساب.
 */
@Composable
fun ProfitReportScreen(
    invoices: List<Invoice>,
    onBack: () -> Unit
) {
    var range by remember { mutableStateOf(ProfitRange.MONTH) }

    val filtered = remember(invoices, range) {
        val cutoff = when (range) {
            ProfitRange.TODAY -> startOfDay(System.currentTimeMillis())
            ProfitRange.WEEK -> startOfDay(System.currentTimeMillis()) - 6L * 24 * 60 * 60 * 1000
            ProfitRange.MONTH -> startOfDay(System.currentTimeMillis()) - 29L * 24 * 60 * 60 * 1000
            ProfitRange.ALL -> Long.MIN_VALUE
        }
        invoices.filter { it.dateMillis >= cutoff && it.status != InvoiceStatus.UNPAID }
    }

    val sales = filtered.sumOf { it.total }
    val cost = filtered.sumOf { it.totalCost }
    val profit = sales - cost
    val withCost = filtered.count { it.hasCostData }
    val margin = if (sales > 0.0) (profit / sales * 100.0) else 0.0

    Scaffold(
        containerColor = Background,
        topBar = { FatouraTopBar(title = "تقرير الأرباح", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfitRange.entries.forEach { option ->
                    FilterChip(
                        selected = range == option,
                        onClick = { range = option },
                        label = { Text(option.label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealSoft,
                            selectedLabelColor = Teal
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.TrendingUp,
                    title = "لا توجد فواتير في هذه الفترة",
                    subtitle = "غيّر النطاق الزمني أو أصدر فاتورة جديدة."
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("صافي الربح", fontSize = 12.sp, color = TextMuted)
                            Text(
                                Fmt.money(profit),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profit >= 0) Green else Color(0xFFC62828)
                            )
                            Spacer(Modifier.height(12.dp))
                            Row {
                                MetricColumn("المبيعات", Fmt.money(sales), Teal, Modifier.weight(1f))
                                MetricColumn("التكلفة", Fmt.money(cost), TextMuted, Modifier.weight(1f))
                                MetricColumn(
                                    "هامش الربح",
                                    "${Fmt.amount(margin)}%",
                                    if (profit >= 0) Green else Color(0xFFC62828),
                                    Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "${filtered.size} فاتورة • $withCost منها مسجّلة التكلفة",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            if (withCost < filtered.size) {
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(TealSoft, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "أضف «سعر شراء الحبة» في دليل السلع لحساب ربح الفواتير الجديدة.",
                                        fontSize = 11.5.sp,
                                        color = Teal
                                    )
                                }
                            }
                        }
                    }
                }

                items(filtered.sortedByDescending { it.dateMillis }, key = { it.id }) { invoice ->
                    ProfitInvoiceRow(invoice)
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = TextMuted)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun ProfitInvoiceRow(invoice: Invoice) {
    val profit = invoice.totalProfit
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GreenSoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#${invoice.number}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    invoice.customerName.ifBlank { "عميل نقدي" },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${Fmt.date(invoice.dateMillis)} • ${invoice.items.size} بند",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Fmt.money(invoice.total), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (profit != null) "ربح ${Fmt.money(profit)}" else "بدون تكلفة",
                    fontSize = 11.sp,
                    color = if (profit != null && profit >= 0) Green else TextMuted
                )
            }
        }
    }
}

/** بداية اليوم الحالي بالمللي ثانية. */
private fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
