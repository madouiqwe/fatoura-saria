package com.madoui.fatoura.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
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
import com.madoui.fatoura.ui.theme.LineColor
import com.madoui.fatoura.ui.theme.Teal
import com.madoui.fatoura.ui.theme.TealDark
import com.madoui.fatoura.ui.theme.TealSoft
import com.madoui.fatoura.ui.theme.TextMuted
import com.madoui.fatoura.ui.theme.TextPrimary
import com.madoui.fatoura.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf(false) }

    if (invoice == null) {
        Scaffold(
            containerColor = Background,
            topBar = { FatouraTopBar(title = "تفاصيل الفاتورة", onBack = onBack) }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("الفاتورة غير موجودة", color = TextMuted)
            }
        }
        return
    }

    fun printDirect() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { InvoicePrinter.printDirectThermal(context, invoice, settings) }
            }
            result.onFailure {
                Toast.makeText(context, "تعذر الطباعة: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val connectPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) printDirect()
        else Toast.makeText(context, "يجب السماح بالاتصال بالبلوتوث للطباعة.", Toast.LENGTH_LONG).show()
    }

    fun requestPrint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            connectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            printDirect()
        }
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
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            InvoicePaper(invoice = invoice, settings = settings)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { requestPrint() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("طباعة XP-P801", fontWeight = FontWeight.Bold)
                }
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
                    Text("طباعة PDF", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    runCatching {
                        val file = InvoicePdf.create(context, invoice, settings)
                        InvoicePrinter.share(context, file)
                    }.onFailure {
                        Toast.makeText(context, "تعذر مشاركة الملف: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("مشاركة PDF", fontWeight = FontWeight.Bold)
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
                TextButton(onClick = { onChangeStatus(InvoiceStatus.UNPAID) }, modifier = Modifier.fillMaxWidth()) {
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

private fun Invoice.isCalculatorQuickInvoice(): Boolean {
    if (items.isEmpty() || items.any { it.productId != null }) return false
    return items.all { item ->
        item.name == "مبلغ إجمالي سريع" || item.name.any { it in "+-×÷*/" }
    }
}

@Composable
private fun InvoicePaper(invoice: Invoice, settings: CompanySettings) {
    val calculatorInvoice = invoice.isCalculatorQuickInvoice()
    val orderedItems = if (calculatorInvoice) {
        invoice.items
    } else {
        invoice.items.sortedWith(
            compareBy<InvoiceItem> {
                if (it.unit == SaleUnit.PACKAGE) {
                    it.unitPrice / it.unitsPerPackage.coerceAtLeast(1).toDouble()
                } else {
                    it.unitPrice
                }
            }.thenBy { it.name }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LineColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Company and invoice identity header
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(TealDark, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(46.dp).background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        settings.name.trim().firstOrNull()?.toString() ?: "ف",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TealDark
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        settings.name.ifBlank { "اسم شركتك" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (settings.phone.isNotBlank()) Text("هاتف: ${settings.phone}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                    if (settings.address.isNotBlank()) Text("العنوان: ${settings.address}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (calculatorInvoice) "فاتورة حسابات" else "فاتورة مبيعات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("رقم ${invoice.number}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                    Text(Fmt.date(invoice.dateMillis), fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F9F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("العميل", fontSize = 9.sp, color = TextMuted)
                        Text(invoice.customerName.ifBlank { "عميل نقدي" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (invoice.customerPhone.isNotBlank()) Text(invoice.customerPhone, fontSize = 10.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("طريقة الدفع", fontSize = 9.sp, color = TextMuted)
                        Text(invoice.paymentMethod.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    StatusBadge(invoice.status)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("تفاصيل الفاتورة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth()
                    .background(TealDark, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (calculatorInvoice) {
                    TableCell("العملية الحسابية", 2.2f, true, TextAlign.Start)
                    TableCell("المبلغ", 1f, true)
                } else {
                    TableCell("الصنف / المنتج", 2.2f, true, TextAlign.Start)
                    TableCell("الكمية", 1.35f, true)
                    TableCell("سعر الوحدة", 1.25f, true)
                    TableCell("الإجمالي", 1.45f, true)
                }
            }

            orderedItems.forEachIndexed { index, item ->
                val rowColor = if (index % 2 == 0) Color.White else Color(0xFFF7FAFA)
                if (calculatorInvoice) {
                    Row(
                        Modifier.fillMaxWidth().background(rowColor).padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (item.name == "مبلغ إجمالي سريع") "مبلغ إجمالي سريع" else item.name,
                            Modifier.weight(2.2f).padding(horizontal = 4.dp),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            Fmt.money(item.lineTotal),
                            Modifier.weight(1f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealDark,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val isPackage = item.unit == SaleUnit.PACKAGE
                    val packageCapacity = item.unitsPerPackage.coerceAtLeast(1)
                    val displayedQuantity = if (isPackage) item.totalPieces else item.quantity
                    val displayedUnitPrice = if (isPackage) item.unitPrice / packageCapacity.toDouble() else item.unitPrice
                    Row(
                        Modifier.fillMaxWidth().background(rowColor).padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(2.2f).padding(horizontal = 4.dp)) {
                            Text(item.name, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (isPackage) {
                                Text("${Fmt.qty(item.quantity)} حزمة × $packageCapacity حبة", fontSize = 8.5.sp, color = TextMuted)
                            }
                        }
                        Text(
                            Fmt.qty(displayedQuantity), Modifier.weight(1.35f), fontSize = 10.sp,
                            color = TextPrimary, textAlign = TextAlign.Center
                        )
                        Text(
                            Fmt.amount(displayedUnitPrice), Modifier.weight(1.25f), fontSize = 10.sp,
                            color = TextPrimary, textAlign = TextAlign.Center
                        )
                        Text(
                            Fmt.amount(item.lineTotal), Modifier.weight(1.45f), fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, color = TealDark, textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider(color = LineColor.copy(alpha = 0.75f))
            }

            Spacer(Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.End) {
                    TotalRow("المجموع الفرعي", Fmt.money(invoice.subtotal))
                    if (invoice.discount > 0.0) {
                        Spacer(Modifier.height(7.dp))
                        TotalRow("الخصم", "− ${Fmt.money(invoice.discount)}")
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = LineColor)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth()
                            .background(TealSoft, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الإجمالي النهائي", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TealDark)
                        Text(Fmt.money(invoice.total), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                    }
                }
            }

            if (!calculatorInvoice) {
                Spacer(Modifier.height(10.dp))
                Text("إجمالي القطع: ${Fmt.qty(invoice.totalPieces)} قطعة", fontSize = 9.5.sp, color = TextMuted)
            }
            if (invoice.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("ملاحظات: ${invoice.notes}", modifier = Modifier.fillMaxWidth(), fontSize = 10.sp, color = TextMuted)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SignaturePlaceholder("توقيع العميل")
                SignaturePlaceholder("ختم المؤسسة")
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = LineColor)
            Spacer(Modifier.height(10.dp))
            Text(
                settings.footerNote.ifBlank { "شكراً لتعاملكم معنا" },
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun SignaturePlaceholder(label: String) {
    Column(
        modifier = Modifier.width(130.dp).height(42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        HorizontalDivider(color = LineColor)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, color = TextMuted)
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
        text,
        modifier = Modifier.weight(weight).padding(horizontal = 4.dp),
        textAlign = align,
        fontSize = if (header) 9.5.sp else 10.sp,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        color = if (header) Color.White else TextPrimary
    )
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.5.sp, color = TextMuted)
        Text(value, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
