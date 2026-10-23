package com.madoui.fatoura.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madoui.fatoura.data.CompanySettings
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceItem
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.data.SaleUnit
import com.madoui.fatoura.print.InvoicePdf
import com.madoui.fatoura.print.InvoicePrinter
import com.madoui.fatoura.ui.components.ConfirmDialog
import com.madoui.fatoura.ui.components.FatouraTopBar
import com.madoui.fatoura.ui.components.StatusBadge
import com.madoui.fatoura.ui.theme.Background
import com.madoui.fatoura.ui.theme.Green
import com.madoui.fatoura.ui.theme.GreenSoft
import com.madoui.fatoura.ui.theme.LineColor
import com.madoui.fatoura.ui.theme.Teal
import com.madoui.fatoura.ui.theme.TealDark
import com.madoui.fatoura.ui.theme.TealSoft
import com.madoui.fatoura.ui.theme.TextMuted
import com.madoui.fatoura.ui.theme.TextPrimary
import com.madoui.fatoura.util.Fmt

@Composable
fun InvoiceDetailScreen(
    invoice: Invoice?,
    settings: CompanySettings,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onChangeStatus: (InvoiceStatus) -> Unit
) {
    val context = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }

    if (invoice == null) {
        Scaffold(
            containerColor = Background,
            topBar = { FatouraTopBar(title = "تفاصيل الفاتورة", onBack = onBack) }
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("الفاتورة غير موجودة", color = TextMuted)
            }
        }
        return
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            FatouraTopBar(
                title = "فاتورة رقم ${invoice.number}",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onEdit(invoice.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل")
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            InvoicePaper(invoice = invoice, settings = settings)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        runCatching {
                            val file = InvoicePdf.create(context, invoice, settings)
                            InvoicePrinter.print(context, file, "فاتورة-${invoice.number}")
                        }.onFailure {
                            Toast.makeText(context, "تعذر إنشاء ملف الطباعة: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("طباعة / PDF", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            val file = InvoicePdf.create(context, invoice, settings)
                            InvoicePrinter.share(context, file)
                        }.onFailure {
                            Toast.makeText(context, "تعذر مشاركة الملف: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مشاركة PDF", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            if (invoice.status != InvoiceStatus.PAID) {
                Button(
                    onClick = { onChangeStatus(InvoiceStatus.PAID) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("تحديد الفاتورة كمدفوعة", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }

            if (invoice.status == InvoiceStatus.PAID) {
                TextButton(
                    onClick = { onChangeStatus(InvoiceStatus.UNPAID) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تحديد كغير مدفوعة", color = TextMuted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "حذف الفاتورة",
            message = "هل تريد حذف الفاتورة رقم ${invoice.number} نهائياً؟",
            onConfirm = {
                showDelete = false
                onDelete(invoice.id)
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun InvoicePaper(invoice: Invoice, settings: CompanySettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LineColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // ------------------------------- الترويسة
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        settings.name.ifBlank { "اسم شركتك" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                    Spacer(Modifier.height(3.dp))
                    Text("هاتف : ${settings.phone}", fontSize = 10.5.sp, color = TextMuted)
                    Text("العنوان : ${settings.address}", fontSize = 10.5.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("فاتورة مبيعات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Teal)
                    Spacer(Modifier.height(4.dp))
                    Text("رقم الفاتورة : ${invoice.number}", fontSize = 10.5.sp, color = TextMuted)
                    Text("التاريخ : ${Fmt.date(invoice.dateMillis)}", fontSize = 10.5.sp, color = TextMuted)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = LineColor, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // ------------------------------- معلومات الفاتورة
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "العميل : ${invoice.customerName.ifBlank { "عميل نقدي" }}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "طريقة الدفع : ${invoice.paymentMethod.label}",
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                    if (invoice.customerPhone.isNotBlank()) {
                        Text("الهاتف : ${invoice.customerPhone}", fontSize = 10.5.sp, color = TextMuted)
                    }
                }
                StatusBadge(invoice.status)
            }

            Spacer(Modifier.height(12.dp))

            // ------------------------------- جدول الأصناف
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(TealDark, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .padding(vertical = 8.dp)
            ) {
                TableCell("الصنف", weight = 2.2f, header = true, align = TextAlign.Start)
                TableCell("الكمية", weight = 1.35f, header = true)
                TableCell("السعر", weight = 1.25f, header = true)
                TableCell("الإجمالي", weight = 1.45f, header = true)
            }

            invoice.items.forEachIndexed { index, item ->
                val background = if (index % 2 == 0) Color.White else Color(0xFFF8FAFA)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(background)
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(2.2f).padding(horizontal = 4.dp)) {
                        Text(
                            item.name,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.unit == SaleUnit.PACKAGE) {
                            Text(
                                "حزمة ${item.unitsPerPackage} حبة",
                                fontSize = 8.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Column(
                        Modifier.weight(1.35f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${Fmt.qty(item.quantity)} ${item.unit.label}",
                            fontSize = 10.sp,
                            color = TextPrimary
                        )
                        if (item.unit == SaleUnit.PACKAGE) {
                            Text(
                                "= ${Fmt.qty(item.totalPieces)} حبة",
                                fontSize = 8.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Text(
                        Fmt.amount(item.unitPrice),
                        modifier = Modifier.weight(1.25f),
                        fontSize = 10.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        Fmt.amount(item.lineTotal),
                        modifier = Modifier.weight(1.45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
                HorizontalDivider(color = LineColor.copy(alpha = 0.7f))
            }

            Spacer(Modifier.height(12.dp))

            // ------------------------------- المجاميع
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                TotalRow("المجموع الفرعي", Fmt.money(invoice.subtotal))
                if (invoice.discount > 0.0) {
                    Spacer(Modifier.height(4.dp))
                    TotalRow("الخصم", "- ${Fmt.money(invoice.discount)}")
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .background(TealSoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الإجمالي النهائي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealDark)
                    Spacer(Modifier.width(16.dp))
                    Text(Fmt.money(invoice.total), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "إجمالي القطع : ${Fmt.qty(invoice.totalPieces)} قطعة",
                fontSize = 9.5.sp,
                color = TextMuted
            )

            Spacer(Modifier.height(16.dp))
            Text(
                settings.footerNote.ifBlank { "شكراً لتعاملكم معنا" },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableCell(
    text: String,
    weight: Float,
    header: Boolean = false,
    align: TextAlign = TextAlign.Center
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 4.dp),
        textAlign = align,
        fontSize = if (header) 10.5.sp else 10.sp,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        color = if (header) Color.White else TextPrimary
    )
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(label, fontSize = 11.5.sp, color = TextMuted)
        Text(value, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
