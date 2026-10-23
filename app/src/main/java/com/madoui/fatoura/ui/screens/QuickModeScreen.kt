package com.madoui.fatoura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceItem
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.data.PaymentMethod
import com.madoui.fatoura.data.SaleUnit
import com.madoui.fatoura.ui.components.FatouraTopBar
import com.madoui.fatoura.ui.theme.Amber
import com.madoui.fatoura.ui.theme.AmberSoft
import com.madoui.fatoura.ui.theme.Background
import com.madoui.fatoura.ui.theme.LineColor
import com.madoui.fatoura.ui.theme.Teal
import com.madoui.fatoura.ui.theme.TealDark
import com.madoui.fatoura.ui.theme.TealSoft
import com.madoui.fatoura.ui.theme.TextMuted
import com.madoui.fatoura.ui.theme.TextPrimary
import com.madoui.fatoura.util.Fmt
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun QuickModeScreen(
    nextInvoiceNumber: String,
    onBack: () -> Unit,
    onSaveInvoice: (Invoice) -> Long,
    onSaved: (Long) -> Unit
) {
    var display by remember { mutableStateOf("0") }
    var accumulator by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<Char?>(null) }
    var resetOnNext by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val currentValue = display.toDoubleOrNull() ?: 0.0

    fun inputDigit(digit: String) {
        display = when {
            resetOnNext || display == "0" -> digit
            else -> display + digit
        }
        resetOnNext = false
    }

    fun inputDot() {
        display = when {
            resetOnNext -> "0."
            !display.contains('.') -> "$display."
            else -> display
        }
        resetOnNext = false
    }

    fun applyOperation(left: Double, right: Double, op: Char): Double = when (op) {
        '+' -> left + right
        '-' -> left - right
        '×' -> left * right
        '÷' -> if (right == 0.0) 0.0 else left / right
        else -> right
    }

    fun chooseOperation(op: Char) {
        val value = display.toDoubleOrNull() ?: 0.0
        val previous = accumulator
        val previousOp = pendingOp
        accumulator = if (previous != null && previousOp != null && !resetOnNext) {
            applyOperation(previous, value, previousOp)
        } else value
        accumulator?.let { display = formatResult(it) }
        pendingOp = op
        resetOnNext = true
    }

    fun equals() {
        val value = display.toDoubleOrNull() ?: 0.0
        val previous = accumulator
        val op = pendingOp
        if (previous != null && op != null) {
            display = formatResult(applyOperation(previous, value, op))
        }
        accumulator = null
        pendingOp = null
        resetOnNext = true
    }

    fun clear() {
        display = "0"
        accumulator = null
        pendingOp = null
        resetOnNext = false
    }

    Scaffold(
        containerColor = Background,
        topBar = { FatouraTopBar(title = "الوضع السريع", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .background(AmberSoft, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("حساب فوري", fontSize = 10.sp, color = Amber, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = Amber)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        display,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(Fmt.money(currentValue), fontSize = 14.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))

            val rows = listOf(
                listOf("C", "⌫", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "=")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        CalcKey(
                            label = key,
                            modifier = Modifier.weight(if (key == "=" && row.size == 3) 2f else 1f),
                            isOperation = key in listOf("÷", "×", "−", "+", "=", "%"),
                            isAccent = key == "=",
                            onClick = {
                                when (key) {
                                    "C" -> clear()
                                    "⌫" -> display = if (display.length <= 1) "0" else display.dropLast(1)
                                    "%" -> display = formatResult((display.toDoubleOrNull() ?: 0.0) / 100.0)
                                    "÷" -> chooseOperation('÷')
                                    "×" -> chooseOperation('×')
                                    "−" -> chooseOperation('-')
                                    "+" -> chooseOperation('+')
                                    "=" -> equals()
                                    "." -> inputDot()
                                    else -> inputDigit(key)
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = currentValue > 0.0
            ) {
                Text("إنشاء فاتورة بهذا المبلغ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "حوّل نتيجة الحساب إلى فاتورة فورية مع إمكانية الطباعة والمشاركة.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSaveDialog) {
        QuickInvoiceDialog(
            amount = currentValue,
            invoiceNumber = nextInvoiceNumber,
            onDismiss = { showSaveDialog = false },
            onConfirm = { customerName, phone ->
                val invoice = Invoice(
                    number = nextInvoiceNumber,
                    customerName = customerName,
                    customerPhone = phone,
                    paymentMethod = PaymentMethod.CASH,
                    status = InvoiceStatus.PAID,
                    items = listOf(
                        InvoiceItem(
                            id = System.currentTimeMillis(),
                            productId = null,
                            name = "مبلغ إجمالي سريع",
                            unit = SaleUnit.PIECE,
                            quantity = 1.0,
                            unitsPerPackage = 1,
                            unitPrice = currentValue
                        )
                    )
                )
                showSaveDialog = false
                onSaved(onSaveInvoice(invoice))
            }
        )
    }
}

@Composable
private fun CalcKey(
    label: String,
    modifier: Modifier = Modifier,
    isOperation: Boolean = false,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    val background = when {
        isAccent -> Teal
        isOperation -> TealSoft
        else -> Color.White
    }
    val contentColor = when {
        isAccent -> Color.White
        isOperation -> TealDark
        else -> TextPrimary
    }

    Box(
        modifier = modifier
            .height(58.dp)
            .background(background, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(Icons.Filled.Backspace, contentDescription = "حذف", tint = contentColor)
        } else {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
        }
    }
}

@Composable
private fun QuickInvoiceDialog(
    amount: Double,
    invoiceNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = { Text("فاتورة سريعة", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column {
                Text(
                    "المبلغ : ${Fmt.money(amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Teal
                )
                Spacer(Modifier.height(4.dp))
                Text("رقم الفاتورة : $invoiceNumber", fontSize = 11.sp, color = TextMuted)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("الهاتف (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), phone.trim()) }) {
                Text("حفظ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}

private fun formatResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    val rounded = (value * 1000.0).roundToLong() / 1000.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.3f", rounded).trimEnd('0').trimEnd('.')
    }
}
