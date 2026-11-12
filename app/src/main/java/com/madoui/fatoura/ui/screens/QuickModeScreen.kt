package com.madoui.fatoura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToLong

private enum class QuickTab(val label: String) {
    PRODUCTS("أصناف سريعة"),
    CALCULATOR("حاسبة")
}

/** سطر داخل سلة البيع السريع. */
private data class QuickLine(
    val key: Long,
    val product: Product,
    val unit: SaleUnit,
    val quantity: Double
) {
    val unitPrice: Double get() = product.priceFor(unit)
    val lineTotal: Double get() = quantity * unitPrice
    val totalPieces: Double
        get() = quantity * if (unit == SaleUnit.PACKAGE) product.packageCapacity.coerceAtLeast(1) else 1
}

private val quickKeyGenerator = AtomicLong(System.currentTimeMillis())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickModeScreen(
    products: List<Product>,
    settings: CompanySettings = CompanySettings(),
    nextInvoiceNumber: String,
    onBack: () -> Unit,
    onSaveInvoice: (Invoice) -> Long,
    onSaved: (Long) -> Unit
) {
    var tab by remember { mutableStateOf(QuickTab.PRODUCTS) }
    var query by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<List<QuickLine>>(emptyList()) }
    var showCart by remember { mutableStateOf(false) }

    val total = lines.sumOf { it.lineTotal }
    val totalPieces = lines.sumOf { it.totalPieces }

    fun addLine(product: Product, unit: SaleUnit) {
        val existing = lines.firstOrNull { it.product.id == product.id && it.unit == unit }
        lines = if (existing != null) {
            lines.map { line ->
                if (line.key == existing.key) line.copy(quantity = line.quantity + 1) else line
            }
        } else {
            lines + QuickLine(
                key = quickKeyGenerator.incrementAndGet(),
                product = product,
                unit = unit,
                quantity = 1.0
            )
        }
    }

    fun updateQuantity(line: QuickLine, quantity: Double) {
        lines = lines.map {
            if (it.key == line.key) it.copy(quantity = quantity.coerceAtLeast(1.0)) else it
        }
    }

    fun updateUnit(line: QuickLine, unit: SaleUnit) {
        val target = lines.firstOrNull {
            it.key != line.key && it.product.id == line.product.id && it.unit == unit
        }
        lines = if (target != null) {
            lines
                .filterNot { it.key == line.key }
                .map {
                    if (it.key == target.key) it.copy(quantity = it.quantity + line.quantity) else it
                }
        } else {
            lines.map { if (it.key == line.key) it.copy(unit = unit) else it }
        }
    }

    fun removeLine(line: QuickLine) {
        lines = lines.filterNot { it.key == line.key }
    }

    fun saveQuickInvoice() {
        if (lines.isEmpty()) return
        val invoice = Invoice(
            number = nextInvoiceNumber,
            paymentMethod = PaymentMethod.CASH,
            status = InvoiceStatus.PAID,
            items = lines.map { line ->
                InvoiceItem(
                    id = line.key,
                    productId = line.product.id,
                    name = line.product.name,
                    unit = line.unit,
                    quantity = line.quantity,
                    unitsPerPackage = line.product.packageCapacity.coerceAtLeast(1),
                    unitPrice = line.unitPrice
                )
            }
        )
        onSaved(onSaveInvoice(invoice))
    }

    Scaffold(
        containerColor = Background,
        topBar = { FatouraTopBar(title = "الوضع السريع", onBack = onBack) },
        bottomBar = {
            if (tab == QuickTab.PRODUCTS) {
                QuickSaveBar(
                    total = total,
                    totalPieces = totalPieces,
                    linesCount = lines.size,
                    enabled = lines.isNotEmpty(),
                    onOpenCart = { showCart = true },
                    onSave = ::saveQuickInvoice
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            QuickTabs(
                selected = tab,
                onSelect = { tab = it }
            )

            when (tab) {
                QuickTab.PRODUCTS -> QuickProductsTab(
                    products = products,
                    cartLines = lines,
                    query = query,
                    onQueryChange = { query = it },
                    onAdd = ::addLine,
                    modifier = Modifier.weight(1f)
                )

                QuickTab.CALCULATOR -> QuickCalculatorTab(
                    nextInvoiceNumber = nextInvoiceNumber,
                    settings = settings,
                    onSaveInvoice = onSaveInvoice,
                    onSaved = onSaved,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showCart) {
        ModalBottomSheet(
            onDismissRequest = { showCart = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            QuickCartSheet(
                lines = lines,
                onQuantityChange = ::updateQuantity,
                onUnitChange = ::updateUnit,
                onRemove = ::removeLine,
                onClear = { lines = emptyList() },
                onDismiss = { showCart = false }
            )
        }
    }
}

@Composable
private fun QuickTabs(selected: QuickTab, onSelect: (QuickTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(LineColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        QuickTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) Teal else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextMuted
                )
            }
        }
    }
}

// ------------------------------------------------------------------ اختيار السلع

@Composable
private fun QuickProductsTab(
    products: List<Product>,
    cartLines: List<QuickLine>,
    query: String,
    onQueryChange: (String) -> Unit,
    onAdd: (Product, SaleUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(products, query) {
        val q = query.trim()
        if (q.isEmpty()) products else products.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true) ||
                it.barcode.contains(q, ignoreCase = true)
        }
    }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("ابحث عن سلعة بالاسم أو الباركود...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Inventory2,
                title = "لا توجد سلع مطابقة",
                subtitle = "جرّب كلمة بحث أخرى أو أضف السلعة من دليل السلع."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "اختر سلعة لإضافتها",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.weight(1f))
                        Text("${filtered.size} سلعة", fontSize = 11.sp, color = TextMuted)
                    }
                }

                items(filtered, key = { it.id }) { product ->
                    QuickProductCard(
                        product = product,
                        inCart = cartLines.filter { it.product.id == product.id },
                        onAddPiece = { onAdd(product, SaleUnit.PIECE) },
                        onAddPackage = { onAdd(product, SaleUnit.PACKAGE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickProductCard(
    product: Product,
    inCart: List<QuickLine>,
    onAddPiece: () -> Unit,
    onAddPackage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(TealSoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Inventory2,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${product.packageCapacity} حبة/حزمة • ${Fmt.amount(product.pricePerPiece)} د.ج للحبة",
                    fontSize = 10.5.sp,
                    color = TextMuted,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "الحزمة: ${Fmt.money(product.packagePrice)}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Teal,
                    maxLines = 1
                )
                if (inCart.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "في السلة: " + inCart.joinToString(" + ") { "${Fmt.qty(it.quantity)} ${it.unit.label}" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            val pieceQty = inCart.firstOrNull { it.unit == SaleUnit.PIECE }?.quantity ?: 0.0
            val packageQty = inCart.firstOrNull { it.unit == SaleUnit.PACKAGE }?.quantity ?: 0.0

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AddUnitButton(
                    label = if (pieceQty > 0) "حبة (${Fmt.qty(pieceQty)})" else "حبة",
                    onClick = onAddPiece
                )
                if (product.packageCapacity > 1) {
                    AddUnitButton(
                        label = if (packageQty > 0) "حزمة (${Fmt.qty(packageQty)})" else "حزمة",
                        onClick = onAddPackage
                    )
                }
            }
        }
    }
}

@Composable
private fun AddUnitButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(TealSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealDark)
        Spacer(Modifier.width(3.dp))
        Icon(Icons.Filled.Add, contentDescription = "إضافة $label", tint = Teal, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun QuickCartCard(
    lines: List<QuickLine>,
    onQuantityChange: (QuickLine, Double) -> Unit,
    onUnitChange: (QuickLine, SaleUnit) -> Unit,
    onRemove: (QuickLine) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "السلة (${lines.size} صنف)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear) {
                    Text("تفريغ", fontSize = 12.sp, color = TextMuted)
                }
            }

            lines.forEachIndexed { index, line ->
                if (index > 0) {
                    HorizontalDivider(color = LineColor.copy(alpha = 0.7f))
                }
                QuickCartLine(
                    line = line,
                    onQuantityChange = onQuantityChange,
                    onUnitChange = onUnitChange,
                    onRemove = onRemove
                )
            }
        }
    }
}

@Composable
private fun QuickCartLine(
    line: QuickLine,
    onQuantityChange: (QuickLine, Double) -> Unit,
    onUnitChange: (QuickLine, SaleUnit) -> Unit,
    onRemove: (QuickLine) -> Unit
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                line.product.name,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = { onRemove(line) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "حذف",
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            CartUnitChip(
                text = "حبة",
                selected = line.unit == SaleUnit.PIECE,
                onClick = { onUnitChange(line, SaleUnit.PIECE) }
            )
            Spacer(Modifier.width(5.dp))
            CartUnitChip(
                text = "حزمة (${line.product.packageCapacity})",
                selected = line.unit == SaleUnit.PACKAGE,
                onClick = { onUnitChange(line, SaleUnit.PACKAGE) }
            )

            Spacer(Modifier.weight(1f))

            QuantityStepper(
                quantity = line.quantity,
                onDecrease = { onQuantityChange(line, line.quantity - 1) },
                onIncrease = { onQuantityChange(line, line.quantity + 1) }
            )

            Spacer(Modifier.width(8.dp))

            Text(
                Fmt.money(line.lineTotal),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TealDark
            )
        }

        if (line.unit == SaleUnit.PACKAGE) {
            Spacer(Modifier.height(3.dp))
            Text(
                "${Fmt.qty(line.quantity)} حزمة × ${line.product.packageCapacity} حبة = ${Fmt.qty(line.totalPieces)} حبة",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun CartUnitChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Teal else LineColor.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else TextMuted
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.background(LineColor.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = "إنقاص", modifier = Modifier.size(15.dp))
        }
        Text(
            Fmt.qty(quantity),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "زيادة", modifier = Modifier.size(15.dp))
        }
    }
}

// ------------------------------------------------------------------ شريط الحفظ

@Composable
private fun QuickSaveBar(
    total: Double,
    totalPieces: Double,
    linesCount: Int,
    enabled: Boolean,
    onOpenCart: () -> Unit,
    onSave: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 10.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (linesCount == 0) "السلة فارغة"
                    else "${linesCount} صنف • ${Fmt.qty(totalPieces)} قطعة",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    Fmt.money(total),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Teal
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (linesCount > 0) TealSoft else LineColor.copy(alpha = 0.5f))
                    .clickable(enabled = linesCount > 0, onClick = onOpenCart)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = "عرض السلة",
                    tint = if (linesCount > 0) Teal else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (linesCount > 0) "السلة ($linesCount)" else "السلة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (linesCount > 0) TealDark else TextMuted
                )
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = onSave,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("حفظ الفاتورة", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------------------------------------------------------ سلة المشتريات

@Composable
private fun QuickCartSheet(
    lines: List<QuickLine>,
    onQuantityChange: (QuickLine, Double) -> Unit,
    onUnitChange: (QuickLine, SaleUnit) -> Unit,
    onRemove: (QuickLine) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            "سلة البيع السريع",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))

        if (lines.isEmpty()) {
            Text(
                "لم تُضف أي سلعة بعد.",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                QuickCartCard(
                    lines = lines,
                    onQuantityChange = onQuantityChange,
                    onUnitChange = onUnitChange,
                    onRemove = onRemove,
                    onClear = onClear
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("متابعة التسوق", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ------------------------------------------------------------------ الحاسبة

/** بند محسوب داخل الحاسبة. يُحوَّل إلى بند فاتورة مباشرة دون أي تحليل نصي. */
private data class CalcEntry(
    val key: Long,
    val label: String,
    val amount: Double,
    val unitPrice: Double,
    val quantity: Double
)

/** رمز داخل تعبير الحاسبة: رقم أو عملية. */
private sealed interface CalcToken {
    data class Num(val value: Double) : CalcToken
    data class Op(val symbol: Char) : CalcToken
}

private fun formatCalcNumber(value: Double): String =
    if (!value.isFinite()) "0" else Fmt.amount(value)

private fun applyCalcOp(left: Double, right: Double, op: Char): Double? = when (op) {
    '+' -> left + right
    '-' -> left - right
    '×', '*' -> left * right
    '÷', '/' -> if (right == 0.0) null else left / right
    else -> right
}

/** حساب التعبير مع احترام أولوية الضرب والقسمة. يعيد null عند القسمة على صفر. */
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
    val hasContent = entries.isNotEmpty() || (currentValue > 0.0 && error == null)

    fun addEntry(label: String, amount: Double, unitPrice: Double, quantity: Double) {
        if (!amount.isFinite() || amount <= 0.0) return
        entries = (entries + CalcEntry(
            key = quickKeyGenerator.incrementAndGet(),
            label = label,
            amount = amount,
            unitPrice = unitPrice,
            quantity = quantity
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
        val items = when {
            entries.isNotEmpty() -> entries.map { entry ->
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
            currentValue > 0.0 -> listOf(
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
            else -> emptyList()
        }
        return Invoice(
            number = nextNumber(),
            customerName = customerName.ifBlank { "عميل نقدي" },
            customerPhone = phone,
            paymentMethod = PaymentMethod.CASH,
            status = InvoiceStatus.PAID,
            kind = InvoiceKind.CALCULATOR,
            items = items
        )
    }

    fun saveAndPrint() {
        if (!hasContent || isPrinting) return
        val invoice = buildInvoice()
        val invoiceId = onSaveInvoice(invoice)
        isPrinting = true
        scope.launch {
            val printed = withContext(Dispatchers.IO) {
                runCatching {
                    InvoicePrinter.printDirectThermal(context, invoice.copy(id = invoiceId), settings)
                }.getOrDefault(false)
            }
            isPrinting = false
            if (printed) {
                Toast.makeText(context, "تم الحفظ والطباعة على XP-P801 ✓", Toast.LENGTH_SHORT).show()
                clearAll()
            } else {
                Toast.makeText(
                    context,
                    "حُفظت الفاتورة ${invoice.number}، وتُعيد الطباعة من شاشة الفاتورة.",
                    Toast.LENGTH_LONG
                ).show()
                onSaved(invoiceId)
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

    Column(
        modifier = modifier
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
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .background(AmberSoft, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("حاسبة سريعة فورية", fontSize = 11.sp, color = Amber, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    if (entries.isNotEmpty()) {
                        Text(
                            "${entries.size} بنود | ${Fmt.qty(entriesPieces)} حبة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        error != null -> error.orEmpty()
                        tokens.isNotEmpty() -> tokensLabel(tokens) + " …"
                        else -> "اكتب: سعر الحبة × عدد الحبات ثم ="
                    },
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 12.sp,
                    color = if (error != null) MaterialTheme.colorScheme.error else TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    display,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = if (display.length > 9) 28.sp else 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (error != null) MaterialTheme.colorScheme.error else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Fmt.money(currentValue), fontSize = 13.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                    if (entries.isNotEmpty()) {
                        Text(
                            "المجموع الكلي: ${Fmt.money(entriesTotal)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (entries.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "بنود الفاتورة الحالية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = { clearAll() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("مسح الكل", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (entry.quantity > 1.0) {
                                    Text(
                                        "${Fmt.qty(entry.quantity)} حبة × ${Fmt.amount(entry.unitPrice)} د.ج",
                                        fontSize = 10.sp,
                                        color = TealDark
                                    )
                                }
                            }
                            Text(
                                Fmt.money(entry.amount),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealDark
                            )
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = { entries = entries.filterNot { it.key == entry.key } },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "حذف السطر",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = LineColor.copy(alpha = 0.7f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "إجمالي الحبات: ${Fmt.qty(entriesPieces)} حبة",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Text(Fmt.money(entriesTotal), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Teal)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Text("مضاعفات سريعة للحبات:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(10, 12, 24, 6, 50, 100).forEach { count ->
                Surface(
                    onClick = { quickMultiply(count) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = TealSoft,
                    tonalElevation = 1.dp
                ) {
                    Text(
                        "× $count",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealDark
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

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
                                "C" -> resetEntry()
                                "⌫" -> backspace()
                                "%" -> display = formatCalcNumber((display.toDoubleOrNull() ?: 0.0) / 100.0)
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

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = { saveAndPrint() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = hasContent && !isPrinting,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1B8755),
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (isPrinting) "جارِ إرسال الأمر للطابعة..." else "طباعة فورية بنقرة واحدة (XP-P801)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        androidx.compose.material3.OutlinedButton(
            onClick = { addCurrentAmount() },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = currentValue > 0.0 && error == null
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("إضافة المبلغ الحالي إلى الفاتورة", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { saveOnly() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = hasContent
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("حفظ سريع", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }

            androidx.compose.material3.OutlinedButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = hasContent
            ) {
                Text("تخصيص عميل...", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showSaveDialog) {
        val invoiceAmount = if (entries.isNotEmpty()) entriesTotal else currentValue
        QuickInvoiceDialog(
            amount = invoiceAmount,
            invoiceNumber = (baseNumber + numberOffset).toString(),
            onDismiss = { showSaveDialog = false },
            onConfirm = { customerName, phone ->
                val invoice = buildInvoice(customerName, phone)
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
            Icon(
                Icons.Filled.Backspace,
                contentDescription = "حذف آخر رقم",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
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
