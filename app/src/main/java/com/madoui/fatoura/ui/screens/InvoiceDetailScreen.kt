package com.madoui.fatoura.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import com.madoui.fatoura.data.calcLineText
import com.madoui.fatoura.print.InvoicePdf
import com.madoui.fatoura.print.InvoicePrinter
import com.madoui.fatoura.ui.components.ConfirmDialog
import com.madoui.fatoura.ui.components.FatouraTopBar
import com.madoui.fatoura.ui.components.StatusBadge
import com.madoui.fatoura.ui.theme.AmberSoft
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
    var isPrinting by remember { mutableStateOf(false) }

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
        if (isPrinting) return
        isPrinting = true
        scope.launch {
            val printed = withContext(Dispatchers.IO) {
                runCatching { InvoicePrinter.printDirectThermal(context, invoice, settings) }.getOrDefault(false)
            }
            isPrinting = false
            if (printed) Toast.makeText(context, "تم إرسال الفاتورة للطابعة ✓", Toast.LENGTH_SHORT).show()
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
            FatouraTopBar(title = "فاتورة رقم ${invoice.number}", onBack = onBack)
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 12.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onEdit(invoice.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تعديل الفاتورة", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showDelete = true },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.45f)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFC62828)
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("حذف", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            InvoicePaper(invoice = invoice, settings = settings)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { requestPrint() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPrinting
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isPrinting) "جارِ الطباعة..." else "طباعة XP-P801", fontWeight = FontWeight.Bold)
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

@Composable
private fun InvoicePaper(invoice: Invoice, settings: CompanySettings) {
    val calculatorInvoice = invoice.isCalculator
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
            InvoiceHeaderBand(invoice = invoice, settings = settings, calculatorInvoice = calculatorInvoice)

            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoTile(
                        label = "العميل",
                        value = invoice.customerName.ifBlank { "عميل نقدي" },
                        hint = invoice.customerPhone.ifBlank { "بدون هاتف" },
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        label = "الدفع",
                        value = invoice.paymentMethod.label,
                        hint = if (calculatorInvoice) "${invoice.items.size} عملية" else "${invoice.items.size} صنف • ${Fmt.qty(invoice.totalPieces)} حبة",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TealSoft)
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (calculatorInvoice) {
                        InvoiceHeaderCell("العملية الحسابية (السعر × الكمية = الإجمالي)", 1f, TextAlign.Start)
                    } else {
                        InvoiceHeaderCell("الصنف / المنتج", 2.2f, TextAlign.Start)
                        InvoiceHeaderCell("الكمية", 1.25f)
                        InvoiceHeaderCell("سعر الحبة", 1.2f)
                        InvoiceHeaderCell("الإجمالي", 1.35f)
                    }
                }

                orderedItems.forEachIndexed { index, item ->
                    InvoiceItemRow(
                        item = item,
                        index = index,
                        calculatorInvoice = calculatorInvoice
                    )
                    DashDivider(Modifier.padding(horizontal = 2.dp))
                }

                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF7FAFA))
                        .padding(14.dp)
                ) {
                    if (!calculatorInvoice) {
                        TotalRow("إجمالي القطع", "${Fmt.qty(invoice.totalPieces)} حبة")
                        Spacer(Modifier.height(7.dp))
                    } else {
                        TotalRow("عدد العمليات", invoice.items.size.toString())
                        Spacer(Modifier.height(7.dp))
                    }
                    TotalRow("المجموع الفرعي", Fmt.money(invoice.subtotal))
                    if (invoice.discount > 0.0) {
                        Spacer(Modifier.height(7.dp))
                        TotalRow("الخصم", "− ${Fmt.money(invoice.discount)}")
                    }
                    Spacer(Modifier.height(10.dp))
                    DashDivider()
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Teal)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "الإجمالي النهائي",
                            modifier = Modifier.weight(1f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            Fmt.money(invoice.total),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                if (invoice.notes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberSoft.copy(alpha = 0.6f))
                            .padding(10.dp)
                    ) {
                        Text("ملاحظات", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A6A00))
                        Spacer(Modifier.height(2.dp))
                        Text(invoice.notes, fontSize = 10.5.sp, color = TextPrimary)
                    }
                }

                if (!calculatorInvoice) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SignaturePlaceholder("توقيع العميل")
                        SignaturePlaceholder("ختم المؤسسة")
                    }
                }

                Spacer(Modifier.height(14.dp))
                DashDivider()
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        settings.footerNote.ifBlank { "شكراً لتعاملكم معنا" },
                        modifier = Modifier.weight(1f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("فاتورة سريعة", fontSize = 9.sp, color = TextMuted.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun InvoiceHeaderBand(invoice: Invoice, settings: CompanySettings, calculatorInvoice: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(TealDark, Teal)))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        settings.name.trim().firstOrNull()?.toString() ?: "ف",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        settings.name.ifBlank { "اسم شركتك" },
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val contacts = listOf(settings.phone, settings.address)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (contacts.isNotBlank()) {
                        Text(
                            contacts,
                            fontSize = 9.5.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (calculatorInvoice) "إيصال حساب" else "فاتورة مبيعات",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "رقم الفاتورة",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        "#${invoice.number}",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Fmt.dateTime(invoice.dateMillis),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                when (invoice.status) {
                                    InvoiceStatus.PAID -> Color(0xFF2E7D32)
                                    InvoiceStatus.UNPAID -> Color(0xFFC62828)
                                    InvoiceStatus.PARTIAL -> Color(0xFF9A6B00)
                                },
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            invoice.status.label,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF6F9F9))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Text(label, fontSize = 8.5.sp, color = TextMuted)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            hint,
            fontSize = 9.sp,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.InvoiceHeaderCell(text: String, weight: Float, align: TextAlign = TextAlign.Center) {
    Text(
        text,
        modifier = Modifier.weight(weight).padding(horizontal = 4.dp),
        textAlign = align,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = TealDark
    )
}

@Composable
private fun InvoiceItemRow(item: InvoiceItem, index: Int, calculatorInvoice: Boolean) {
    val rowColor = if (index % 2 == 0) Color.White else Color(0xFFFBFCFC)
    if (calculatorInvoice) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(rowColor)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayName = calcLineText(item)
            Column(Modifier.weight(1f).padding(horizontal = 2.dp)) {
                Text(
                    displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.quantity > 1.0) {
                    Text(
                        "${Fmt.qty(item.quantity)} حبة × ${Fmt.amount(item.unitPrice)} د.ج",
                        fontSize = 8.5.sp,
                        color = TextMuted
                    )
                }
            }
        }
    } else {
        val isPackage = item.unit == SaleUnit.PACKAGE
        val packageCapacity = item.unitsPerPackage.coerceAtLeast(1)
        Row(
            Modifier
                .fillMaxWidth()
                .background(rowColor)
                .padding(vertical = 9.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(2.2f).padding(horizontal = 2.dp)) {
                Text(
                    item.name,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isPackage) {
                    Text(
                        "${Fmt.qty(item.quantity)} حزمة × $packageCapacity حبة",
                        fontSize = 8.5.sp,
                        color = TextMuted
                    )
                }
            }
            Text(
                "${Fmt.qty(item.totalPieces)}",
                Modifier.weight(1.25f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                Fmt.amount(item.piecePrice),
                Modifier.weight(1.2f),
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            Text(
                Fmt.amount(item.lineTotal),
                Modifier.weight(1.35f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = TealDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DashDivider(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = LineColor,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))
        )
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
