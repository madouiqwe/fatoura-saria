package com.madoui.fatoura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.ui.components.EmptyState
import com.madoui.fatoura.ui.components.FatouraTopBar
import com.madoui.fatoura.ui.components.StatusBadge
import com.madoui.fatoura.ui.theme.Background
import com.madoui.fatoura.ui.theme.Blue
import com.madoui.fatoura.ui.theme.BlueSoft
import com.madoui.fatoura.ui.theme.Green
import com.madoui.fatoura.ui.theme.LineColor
import com.madoui.fatoura.ui.theme.TextMuted
import com.madoui.fatoura.ui.theme.TextPrimary
import com.madoui.fatoura.util.Fmt

private enum class InvoiceFilter(val label: String) {
    ALL("الكل"),
    PAID("مدفوعة"),
    UNPAID("غير مدفوعة"),
    PARTIAL("جزئية")
}

@Composable
fun InvoiceListScreen(
    invoices: List<Invoice>,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onOpen: (Long) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(InvoiceFilter.ALL) }

    val filtered = remember(invoices, query, filter) {
        invoices
            .filter { invoice ->
                when (filter) {
                    InvoiceFilter.ALL -> true
                    InvoiceFilter.PAID -> invoice.status == InvoiceStatus.PAID
                    InvoiceFilter.UNPAID -> invoice.status == InvoiceStatus.UNPAID
                    InvoiceFilter.PARTIAL -> invoice.status == InvoiceStatus.PARTIAL
                }
            }
            .filter { invoice ->
                val q = query.trim()
                q.isEmpty() || invoice.number.contains(q) ||
                    invoice.customerName.contains(q, ignoreCase = true) ||
                    invoice.customerPhone.contains(q)
            }
            .sortedByDescending { it.createdAt }
    }

    Scaffold(
        containerColor = Background,
        topBar = { FatouraTopBar(title = "الفواتير المحفوظة", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                containerColor = Green,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("فاتورة جديدة", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("بحث برقم الفاتورة أو اسم العميل...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InvoiceFilter.entries.forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item.label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueSoft,
                            selectedLabelColor = Blue
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Description,
                    title = "لا توجد فواتير",
                    subtitle = "أنشئ أول فاتورة لك وستظهر هنا مع إمكانية البحث والطباعة في أي وقت."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { invoice ->
                        InvoiceCard(invoice = invoice, onClick = { onOpen(invoice.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(BlueSoft, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Description, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "فاتورة #${invoice.number}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(invoice.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    invoice.customerName.ifBlank { "عميل نقدي" },
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Fmt.date(invoice.dateMillis), fontSize = 11.sp, color = TextMuted)
                    Box(
                        Modifier
                            .height(12.dp)
                            .width(1.dp)
                            .background(LineColor)
                    )
                    Text(invoice.paymentMethod.label, fontSize = 11.sp, color = TextMuted)
                    Box(
                        Modifier
                            .height(12.dp)
                            .width(1.dp)
                            .background(LineColor)
                    )
                    Text("${Fmt.qty(invoice.totalPieces)} قطعة", fontSize = 11.sp, color = TextMuted)
                }
            }

            Text(
                Fmt.amount(invoice.total),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Green
            )
        }
    }
}
