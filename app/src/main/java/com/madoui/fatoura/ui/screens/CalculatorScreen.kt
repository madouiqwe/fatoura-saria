package com.madoui.fatoura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.madoui.fatoura.data.CompanySettings
import com.madoui.fatoura.data.DIRECT_AMOUNT_LABEL
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceItem
import com.madoui.fatoura.data.InvoiceKind
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.data.PaymentMethod
import com.madoui.fatoura.data.Product
import com.madoui.fatoura.data.SaleUnit
import com.madoui.fatoura.print.InvoicePrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.madoui.fatoura.ui.components.EmptyState
import com.madoui.fatoura.ui.components.FatouraTopBar
import com.madoui.fatoura.ui.components.ReceiptPreviewDialog
import com.madoui.fatoura.ui.theme.Amber
import com.madoui.fatoura.ui.theme.AmberSoft
import com.madoui.fatoura.ui.theme.Background
import com.madoui.fatoura.ui.theme.Danger
import com.madoui.fatoura.ui.theme.DangerSoft
import com.madoui.fatoura.ui.theme.LineColor
import com.madoui.fatoura.ui.theme.Teal
import com.madoui.fatoura.ui.theme.TealDark
import com.madoui.fatoura.ui.theme.TealSoft
import com.madoui.fatoura.ui.theme.TextMuted
import com.madoui.fatoura.ui.theme.TextPrimary
import com.madoui.fatoura.util.Fmt
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.round

/** شاشة الحاسبة السريعة: خانة مستقلة داخل التطبيق للحساب الفوري وإصدار الفاتورة. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCalculatorScreen(
    nextInvoiceNumber: String,
    settings: CompanySettings,
    onBack: () -> Unit,
    onSaveInvoice: (Invoice) -> Long,
    onSaved: (Long) -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = { FatouraTopBar(title = "الحاسبة السريعة", onBack = onBack) }
    ) { padding ->
        QuickCalculatorTab(
            nextInvoiceNumber = nextInvoiceNumber,
            settings = settings,
            onSaveInvoice = onSaveInvoice,
            onSaved = onSaved,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        )
    }
}

// ------------------------------------------------------------------ الحاسبة

/** بند محسوب داخل الحاسبة، يُحوَّل إلى بند فاتورة مباشرة دون تحليل نصي. */
private data class CalcEntry(
    val key: Long,
    val label: String,
    val unitPrice: Double,
    val quantity: Double
) {
    /** الإجمالي يُشتق دائماً من السعر × الكمية، فتبقى الشاشة مطابقة للمطبوع. */
    val amount: Double get() = moneyRound(unitPrice * quantity)
}

/**
 * يعيد صياغة بيان السطر بعد تعديل الكمية أو السعر:
 * «250 × 10» تصبح «250 × 12»، والعملية المركّبة تتحوّل إلى المبلغ النهائي،
 * والمبلغ المباشر يحتفظ بتسميته.
 */
private fun calcEntryLabel(originalLabel: String, unitPrice: Double, quantity: Double): String = when {
    originalLabel.trim() == DIRECT_AMOUNT_LABEL -> DIRECT_AMOUNT_LABEL
    quantity > 1.0 -> "${formatCalcNumber(unitPrice)} × ${formatCalcNumber(quantity)}"
    else -> formatCalcNumber(unitPrice)
}

/**
 * نص العملية الكامل بنتيجتها: «250 × 10 = 2500».
 * يظهر في قائمة بنود الحاسبة وفي الإيصال الحراري.
 */
private fun calcEntryEquation(originalLabel: String, unitPrice: Double, quantity: Double): String {
    val label = originalLabel.trim()
    val total = moneyRound(unitPrice * quantity)
    if (quantity > 1.0) {
        return "${formatCalcNumber(unitPrice)} × ${formatCalcNumber(quantity)} = ${formatCalcNumber(total)}"
    }
    if (label == DIRECT_AMOUNT_LABEL) return "${DIRECT_AMOUNT_LABEL} = ${formatCalcNumber(total)}"
    val digitsOnly = label.isNotEmpty() && label.all { it.isDigit() || it == '.' }
    return if (digitsOnly) formatCalcNumber(total) else "$label = ${formatCalcNumber(total)}"
}

/** رمز داخل تعبير الحاسبة: رقم أو عملية. */
private sealed interface CalcToken {
    data class Num(val value: Double) : CalcToken
    data class Op(val symbol: Char) : CalcToken
}

/** ينسّق رقم الحاسبة حتى أربع منازل دون أصفار زائدة. */
private fun formatCalcNumber(value: Double): String {
    if (!value.isFinite()) return "0"
    val rounded = round(value * 10_000.0) / 10_000.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.4f", rounded).trimEnd('0').trimEnd('.')
    }
}

/** يقرّب المبالغ إلى منزلتين عشريتين كما تُعرض في الفاتورة. */
private fun moneyRound(value: Double): Double = round(value * 100.0) / 100.0

private fun applyCalcOp(left: Double, right: Double, op: Char): Double? = when (op) {
    '+' -> left + right
    '-' -> left - right
    '×', '*' -> left * right
    '÷', '/' -> if (right == 0.0) null else left / right
    else -> right
}

/** يحسب التعبير مع أولوية الضرب والقسمة، ويعيد null عند القسمة على صفر. */
private fun evaluateTokens(tokens: List<CalcToken>): Double? {
    if (tokens.isEmpty()) return null
    val numbers = mutableListOf<Double>()
    val operators = mutableListOf<Char>()
    tokens.forEach { token ->
        when (token) {
            is CalcToken.Num -> numbers.add(token.value)
            is CalcToken.Op -> operators.add(token.symbol)
        }
    }
    if (numbers.size != operators.size + 1) return null

    var index = 0
    while (index < operators.size) {
        val op = operators[index]
        if (op == '×' || op == '*' || op == '÷' || op == '/') {
            val folded = applyCalcOp(numbers[index], numbers[index + 1], op) ?: return null
            numbers[index] = folded
            numbers.removeAt(index + 1)
            operators.removeAt(index)
        } else {
            index++
        }
    }

    var total = numbers.first()
    operators.forEachIndexed { position, op ->
        total = applyCalcOp(total, numbers[position + 1], op) ?: return null
    }
    return total
}

private fun tokensLabel(tokens: List<CalcToken>): String = tokens.joinToString(" ") { token ->
    when (token) {
        is CalcToken.Num -> formatCalcNumber(token.value)
        is CalcToken.Op -> token.symbol.toString()
    }
}

/**
 * يستخرج سعر الحبة وعدد الحبات من تعبير الضرب البسيط «سعر × عدد».
 * التعبيرات الأخرى تُحسب كسطر مبلغ واحد بلا كمية.
 */
private fun priceAndQuantity(tokens: List<CalcToken>, amount: Double): Pair<Double, Double> {
    val simpleProduct = tokens.size == 3 &&
        tokens[0] is CalcToken.Num &&
        tokens[1] is CalcToken.Op &&
        tokens[2] is CalcToken.Num &&
        (tokens[1] as CalcToken.Op).symbol.let { it == '×' || it == '*' }
    if (simpleProduct) {
        val price = (tokens[0] as CalcToken.Num).value
        val count = (tokens[2] as CalcToken.Num).value
        if (price > 0.0 && count > 0.0 && count % 1.0 == 0.0 && count <= 100_000.0) {
            return price to count
        }
    }
    return amount to 1.0
}

@Composable
private fun QuickCalculatorTab(
    nextInvoiceNumber: String,
    settings: CompanySettings,
    onSaveInvoice: (Invoice) -> Long,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var display by remember { mutableStateOf("0") }
    var tokens by remember { mutableStateOf<List<CalcToken>>(emptyList()) }
    var resetOnNext by remember { mutableStateOf(false) }
    var evaluated by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<CalcEntry>>(emptyList()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var pendingPrint by remember { mutableStateOf<Invoice?>(null) }
    var editingEntry by remember { mutableStateOf<CalcEntry?>(null) }

    // ترقيم محلي متزايد حتى لا يتكرر رقم الفاتورة عند الطباعة والحفظ المتتالي.
    val baseNumber = remember { nextInvoiceNumber.toLongOrNull() ?: 0L }
    var numberOffset by remember { mutableStateOf(0L) }
    fun nextNumber(): String {
        val number = (baseNumber + numberOffset).toString()
        numberOffset += 1
        return number
    }

    val currentValue = display.toDoubleOrNull() ?: 0.0
    val entriesTotal = entries.sumOf { it.amount }
    val entriesPieces = entries.sumOf { it.quantity }
    val pendingAmount = tokens.isNotEmpty() && currentValue > 0.0 && error == null
    val hasContent = entries.isNotEmpty() || (currentValue > 0.0 && error == null)

    fun addEntry(label: String, amount: Double, unitPrice: Double, quantity: Double) {
        val qty = if (quantity > 0.0 && quantity.isFinite()) quantity else 1.0
        val price = when {
            unitPrice > 0.0 -> moneyRound(unitPrice)
            amount > 0.0 -> moneyRound(amount / qty)
            else -> 0.0
        }
        if (!price.isFinite() || price <= 0.0) return
        entries = (entries + CalcEntry(
            key = quickKeyGenerator.incrementAndGet(),
            label = label,
            unitPrice = price,
            quantity = qty
        )).takeLast(50)
    }

    fun resetEntry() {
        display = "0"
        tokens = emptyList()
        resetOnNext = false
        evaluated = false
        error = null
    }

    fun clearAll() {
        resetEntry()
        entries = emptyList()
    }

    fun inputDigit(digit: String) {
        if (error != null) resetEntry()
        if (evaluated) {
            tokens = emptyList()
            evaluated = false
        }
        display = when {
            resetOnNext || display == "0" -> digit
            display.length >= 12 -> display
            else -> display + digit
        }
        resetOnNext = false
    }

    fun inputDot() {
        if (error != null) resetEntry()
        if (evaluated) {
            tokens = emptyList()
            evaluated = false
        }
        display = when {
            resetOnNext -> "0."
            !display.contains('.') -> "$display."
            else -> display
        }
        resetOnNext = false
    }

    fun backspace() {
        if (error != null) {
            resetEntry()
            return
        }
        if (evaluated || resetOnNext) {
            resetEntry()
            return
        }
        display = if (display.length <= 1 || (display.length == 2 && display.startsWith("-"))) "0" else display.dropLast(1)
    }

    fun chooseOperation(op: Char) {
        if (error != null) return
        val value = display.toDoubleOrNull() ?: 0.0
        tokens = when {
            tokens.isEmpty() -> listOf(CalcToken.Num(value), CalcToken.Op(op))
            tokens.last() is CalcToken.Op -> tokens.dropLast(1) + CalcToken.Op(op)
            else -> tokens + CalcToken.Op(op)
        }
        val running = evaluateTokens(tokens.dropLast(1))
        if (running == null) {
            error = "لا يمكن القسمة على صفر"
            tokens = emptyList()
            display = "0"
            resetOnNext = true
            return
        }
        display = formatCalcNumber(running)
        resetOnNext = true
        evaluated = false
    }

    /** النسبة: تحسب على ناتج العملية الجارية إن وُجدت، وإلا تقسم الرقم الحالي على 100. */
    fun percent() {
        if (error != null) return
        val value = display.toDoubleOrNull() ?: 0.0
        val base = if (tokens.isNotEmpty() && tokens.last() is CalcToken.Op) {
            evaluateTokens(tokens.dropLast(1))
        } else {
            null
        }
        val result = if (base != null) base * value / 100.0 else value / 100.0
        if (!result.isFinite()) {
            error = "قيمة غير صالحة"
            return
        }
        display = formatCalcNumber(result)
        resetOnNext = false
        evaluated = false
    }

    fun equals() {
        if (error != null) return
        val value = display.toDoubleOrNull() ?: 0.0
        val full = if (tokens.isEmpty()) listOf(CalcToken.Num(value)) else tokens + CalcToken.Num(value)
        val result = evaluateTokens(full)
        if (result == null) {
            error = "لا يمكن القسمة على صفر"
            tokens = emptyList()
            display = "0"
            resetOnNext = true
            return
        }
        val label = if (full.size == 1) DIRECT_AMOUNT_LABEL else tokensLabel(full)
        val (price, quantity) = priceAndQuantity(full, result)
        addEntry(label, result, price, quantity)
        display = formatCalcNumber(result)
        tokens = emptyList()
        resetOnNext = true
        evaluated = true
    }

    fun quickMultiply(count: Int) {
        if (error != null) return
        val price = display.toDoubleOrNull() ?: 0.0
        if (price <= 0.0) return
        val result = price * count
        addEntry(
            label = "${formatCalcNumber(price)} × $count",
            amount = result,
            unitPrice = price,
            quantity = count.toDouble()
        )
        display = formatCalcNumber(result)
        tokens = emptyList()
        resetOnNext = true
        evaluated = true
    }

    fun addCurrentAmount() {
        if (error != null) return
        val value = display.toDoubleOrNull() ?: 0.0
        if (value <= 0.0) return
        addEntry(DIRECT_AMOUNT_LABEL, value, value, 1.0)
        resetEntry()
    }

    fun buildInvoice(customerName: String = "عميل نقدي", phone: String = ""): Invoice {
        val saved = entries.map { entry ->
            InvoiceItem(
                id = entry.key,
                productId = null,
                name = entry.label,
                unit = SaleUnit.PIECE,
                quantity = entry.quantity,
                unitsPerPackage = 1,
                unitPrice = entry.unitPrice
            )
        }
        // العملية غير المنتهية بـ = تُضاف كسطر أخير حتى لا يضيع المبلغ أثناء الطباعة.
        val pending = if (pendingAmount) {
            listOf(
                InvoiceItem(
                    id = quickKeyGenerator.incrementAndGet(),
                    productId = null,
                    name = tokensLabel(tokens).ifBlank { DIRECT_AMOUNT_LABEL },
                    unit = SaleUnit.PIECE,
                    quantity = 1.0,
                    unitsPerPackage = 1,
                    unitPrice = currentValue
                )
            )
        } else if (saved.isEmpty() && currentValue > 0.0) {
            listOf(
                InvoiceItem(
                    id = quickKeyGenerator.incrementAndGet(),
                    productId = null,
                    name = DIRECT_AMOUNT_LABEL,
                    unit = SaleUnit.PIECE,
                    quantity = 1.0,
                    unitsPerPackage = 1,
                    unitPrice = currentValue
                )
            )
        } else {
            emptyList()
        }
        return Invoice(
            number = nextNumber(),
            customerName = customerName.ifBlank { "عميل نقدي" },
            customerPhone = phone,
            paymentMethod = PaymentMethod.CASH,
            status = InvoiceStatus.PAID,
            kind = InvoiceKind.CALCULATOR,
            items = saved + pending
        )
    }

    fun saveAndPrint() {
        if (!hasContent || isPrinting) return
        val invoice = buildInvoice()
        val invoiceId = onSaveInvoice(invoice)
        // تُحفظ الفاتورة ثم تُعرض المعاينة قبل الإرسال للطابعة.
        pendingPrint = invoice.copy(id = invoiceId)
        showPreview = true
    }

    fun printPending() {
        val invoice = pendingPrint ?: return
        if (isPrinting) return
        isPrinting = true
        scope.launch {
            val printed = withContext(Dispatchers.IO) {
                runCatching { InvoicePrinter.printDirectThermal(context, invoice, settings) }
                    .getOrDefault(false)
            }
            isPrinting = false
            if (printed) {
                showPreview = false
                pendingPrint = null
                clearAll()
                Toast.makeText(context, "تم الحفظ والطباعة على XP-P801 ✓", Toast.LENGTH_SHORT).show()
            } else {
                showPreview = false
                pendingPrint = null
                Toast.makeText(
                    context,
                    "حُفظت الفاتورة ${invoice.number}، وتُعيد الطباعة من شاشة الفاتورة.",
                    Toast.LENGTH_LONG
                ).show()
                onSaved(invoice.id)
            }
        }
    }

    fun saveOnly() {
        if (!hasContent) return
        val invoice = buildInvoice()
        val invoiceId = onSaveInvoice(invoice)
        Toast.makeText(context, "تم حفظ الفاتورة رقم ${invoice.number}", Toast.LENGTH_SHORT).show()
        onSaved(invoiceId)
    }

    fun handleKey(key: String) {
        when (key) {
            "C" -> resetEntry()
            "⌫" -> backspace()
            "%" -> percent()
            "÷" -> chooseOperation('÷')
            "×" -> chooseOperation('×')
            "−" -> chooseOperation('-')
            "+" -> chooseOperation('+')
            "=" -> equals()
            "." -> inputDot()
            else -> inputDigit(key)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // ارتفاع الشاشة يحدد حجم المفاتيح حتى تبقى لوحة الأرقام ظاهرة دائماً دون تمرير.
        val available = maxHeight
        val dense = available < 620.dp
        val keyHeight = when {
            available < 500.dp -> 36.dp
            available < 560.dp -> 41.dp
            available < 640.dp -> 45.dp
            else -> 48.dp
        }
        val barHeight = if (available < 560.dp) 42.dp else 46.dp
        val keysGap = if (dense) 5.dp else 7.dp

        Column(Modifier.fillMaxSize()) {
            CalcDisplayCard(
                display = display,
                expression = if (tokens.isNotEmpty()) tokensLabel(tokens) + " " + display else "",
                error = error,
                entriesCount = entries.size,
                entriesPieces = entriesPieces,
                entriesTotal = entriesTotal,
                dense = dense,
                onCustomCustomer = { showSaveDialog = true }
            )

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "كل عملية تنتهي بـ = تصبح بنداً في الفاتورة",
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(3.dp))
                        Text("350 × 12 =", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TealDark)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    item(key = "entries-header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "بنود الفاتورة (${entries.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "مسح الكل",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { clearAll() }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    itemsIndexed(entries, key = { _, entry -> entry.key }) { _, entry ->
                        CalcEntryRow(
                            entry = entry,
                            onEdit = { editingEntry = entry },
                            onRemove = { entries = entries.filterNot { it.key == entry.key } }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(6, 10, 12, 24, 50, 100).forEach { count ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (dense) 27.dp else 30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TealSoft)
                            .clickable { quickMultiply(count) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealDark)
                    }
                }
            }

            Spacer(Modifier.height(keysGap))
            CalcKeypad(keyHeight = keyHeight, gap = keysGap, onKey = ::handleKey)
            Spacer(Modifier.height(keysGap))
            CalcActionBar(
                canAddCurrent = pendingAmount || (entries.isEmpty() && currentValue > 0.0 && error == null),
                hasContent = hasContent,
                isPrinting = isPrinting,
                height = barHeight,
                onAddCurrent = { addCurrentAmount() },
                onSaveOnly = { saveOnly() },
                onPrint = { saveAndPrint() }
            )
        }
    }

    if (showSaveDialog) {
        val invoiceAmount = entriesTotal + if (pendingAmount) currentValue else 0.0
        QuickInvoiceDialog(
            amount = if (invoiceAmount > 0.0) invoiceAmount else currentValue,
            invoiceNumber = (baseNumber + numberOffset).toString(),
            onDismiss = { showSaveDialog = false },
            onConfirm = { customerName, phone ->
                val invoice = buildInvoice(customerName, phone)
                showSaveDialog = false
                onSaved(onSaveInvoice(invoice))
            }
        )
    }

    editingEntry?.let { entry ->
        CalcEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onConfirm = { label, price, quantity ->
                entries = entries.map {
                    if (it.key == entry.key) {
                        it.copy(label = label, unitPrice = moneyRound(price), quantity = quantity)
                    } else {
                        it
                    }
                }
                editingEntry = null
            }
        )
    }

    pendingPrint?.let { invoice ->
        ReceiptPreviewDialog(
            invoice = invoice,
            settings = settings,
            isPrinting = isPrinting,
            onDismiss = { showPreview = false; pendingPrint = null },
            onPrint = { printPending() }
        )
    }
}

@Composable
private fun CalcDisplayCard(
    display: String,
    expression: String,
    error: String?,
    entriesCount: Int,
    entriesPieces: Double,
    entriesTotal: Double,
    dense: Boolean,
    onCustomCustomer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (dense) 18.dp else 24.dp))
            .background(Brush.linearGradient(listOf(TealDark, Teal)))
            .padding(horizontal = 16.dp, vertical = if (dense) 10.dp else 14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text("حاسبة فورية", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                if (entriesCount > 0) {
                    Text(
                        "$entriesCount بند • ${Fmt.qty(entriesPieces)} حبة",
                        fontSize = 9.5.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = onCustomCustomer, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "تخصيص عميل",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                expression.ifBlank { " " },
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            Text(
                display,
                modifier = Modifier.fillMaxWidth(),
                fontSize = when {
                    display.length > 9 -> if (dense) 26.sp else 30.sp
                    dense -> 33.sp
                    else -> 38.sp
                },
                fontWeight = FontWeight.Bold,
                color = if (error != null) Color(0xFFFFD7D7) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            if (error != null) {
                Text(
                    error,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 10.5.sp,
                    color = Color(0xFFFFD7D7),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(if (dense) 5.dp else 8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
            Spacer(Modifier.height(if (dense) 4.dp else 7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("إجمالي الفاتورة", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.78f))
                Spacer(Modifier.weight(1f))
                Text(
                    Fmt.money(entriesTotal),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CalcEntryRow(
    entry: CalcEntry,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(start = 12.dp, end = 2.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                calcEntryEquation(entry.label, entry.unitPrice, entry.quantity),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.quantity > 1.0) {
                Text(
                    "${Fmt.qty(entry.quantity)} حبة × ${Fmt.amount(entry.unitPrice)} د.ج",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
        Text(
            Fmt.money(entry.amount),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onEdit)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = TealDark
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "حذف البند",
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private enum class CalcKeyKind { DIGIT, ACTION, OPERATOR, ACCENT, DANGER }

@Composable
private fun CalcKeypad(keyHeight: Dp, gap: Dp, onKey: (String) -> Unit) {
    val rows = listOf(
        listOf("C", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                row.forEach { key ->
                    val kind = when {
                        key == "=" -> CalcKeyKind.ACCENT
                        key == "C" -> CalcKeyKind.DANGER
                        key in listOf("÷", "×", "−", "+") -> CalcKeyKind.OPERATOR
                        key == "⌫" || key == "%" -> CalcKeyKind.ACTION
                        else -> CalcKeyKind.DIGIT
                    }
                    CalcKey(
                        label = key,
                        kind = kind,
                        keyHeight = keyHeight,
                        modifier = Modifier.weight(if (key == "0") 2f else 1f),
                        onClick = { onKey(key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcKey(
    label: String,
    kind: CalcKeyKind,
    keyHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = when (kind) {
        CalcKeyKind.ACCENT -> Teal
        CalcKeyKind.OPERATOR -> TealSoft
        CalcKeyKind.DANGER -> DangerSoft
        CalcKeyKind.ACTION -> Color(0xFFF1F3F5)
        CalcKeyKind.DIGIT -> Color.White
    }
    val contentColor = when (kind) {
        CalcKeyKind.ACCENT -> Color.White
        CalcKeyKind.OPERATOR -> TealDark
        CalcKeyKind.DANGER -> Danger
        else -> TextPrimary
    }
    val corner = if (keyHeight < 44.dp) 11.dp else 14.dp

    Box(
        modifier = modifier
            .height(keyHeight)
            .clip(RoundedCornerShape(corner))
            .background(background)
            .border(
                width = 1.dp,
                color = if (kind == CalcKeyKind.DIGIT) LineColor else Color.Transparent,
                shape = RoundedCornerShape(corner)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(
                Icons.Filled.Backspace,
                contentDescription = "حذف آخر رقم",
                tint = contentColor,
                modifier = Modifier.size(if (keyHeight < 44.dp) 17.dp else 19.dp)
            )
        } else {
            Text(
                label,
                fontSize = if (keyHeight < 44.dp) 17.sp else 19.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CalcActionBar(
    canAddCurrent: Boolean,
    hasContent: Boolean,
    isPrinting: Boolean,
    height: Dp,
    onAddCurrent: () -> Unit,
    onSaveOnly: () -> Unit,
    onPrint: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        androidx.compose.material3.OutlinedButton(
            onClick = onAddCurrent,
            enabled = canAddCurrent,
            modifier = Modifier
                .weight(1f)
                .height(height),
            shape = RoundedCornerShape(13.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("إضافة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        androidx.compose.material3.OutlinedButton(
            onClick = onSaveOnly,
            enabled = hasContent,
            modifier = Modifier
                .weight(1f)
                .height(height),
            shape = RoundedCornerShape(13.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("حفظ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }

        Button(
            onClick = onPrint,
            enabled = hasContent && !isPrinting,
            modifier = Modifier
                .weight(1.3f)
                .height(height),
            shape = RoundedCornerShape(13.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1B8755),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (isPrinting) "جارٍ..." else "طباعة",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CalcEntryDialog(
    entry: CalcEntry,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit
) {
    var qtyText by remember(entry.key) { mutableStateOf(Fmt.qty(entry.quantity)) }
    var priceText by remember(entry.key) { mutableStateOf(Fmt.amount(entry.unitPrice)) }
    var totalText by remember(entry.key) { mutableStateOf(Fmt.amount(entry.amount)) }
    var invalid by remember(entry.key) { mutableStateOf<String?>(null) }

    fun recomputeTotal() {
        val price = Fmt.parseNumber(priceText)
        val qty = Fmt.parseNumber(qtyText)
        if (price != null && qty != null && price >= 0.0 && qty > 0.0) {
            totalText = Fmt.amount(moneyRound(price * qty))
        }
    }

    val previewPrice = Fmt.parseNumber(priceText) ?: 0.0
    val previewQty = Fmt.parseNumber(qtyText) ?: 0.0
    val previewTotal = moneyRound(previewPrice * previewQty)
    val previewLabel = calcEntryLabel(entry.label, previewPrice, previewQty)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = { Text("تعديل البند", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "عدّل الكمية أو سعر الحبة، والإجمالي يتحدّث فوراً — وهذا ما يُطبَع في الفاتورة.",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = {
                            qtyText = it
                            invalid = null
                            recomputeTotal()
                        },
                        label = { Text("الكمية") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            priceText = it
                            invalid = null
                            recomputeTotal()
                        },
                        label = { Text("سعر الحبة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { typed ->
                        totalText = typed
                        invalid = null
                        // كتابة الإجمالي مباشرةً تُعيد توزيعه على الكمية للحفاظ على سعر حبة صحيح.
                        val total = Fmt.parseNumber(typed)
                        val qty = Fmt.parseNumber(qtyText)
                        if (total != null && qty != null && qty > 0.0) {
                            priceText = Fmt.amount(moneyRound(total / qty))
                        }
                    },
                    label = { Text("الإجمالي (د.ج)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = invalid != null,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealSoft)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        "سطر الفاتورة: $previewLabel = ${Fmt.money(previewTotal)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                }
                if (invalid != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(invalid!!, fontSize = 11.sp, color = Danger)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = Fmt.parseNumber(priceText)
                val qty = Fmt.parseNumber(qtyText)
                when {
                    qty == null || qty <= 0.0 -> invalid = "أدخل كمية أكبر من صفر"
                    price == null || price <= 0.0 -> invalid = "أدخل سعراً أكبر من صفر"
                    else -> onConfirm(calcEntryLabel(entry.label, price, qty), price, qty)
                }
            }) {
                Text("حفظ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
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
        title = {
            Text("حفظ فاتورة سريعة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                Text("المبلغ: ${Fmt.money(amount)}", fontWeight = FontWeight.Bold, color = Teal)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف العميل") },
                    placeholder = { Text("05xxxxxxxx") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
