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

internal val quickKeyGenerator = AtomicLong(System.currentTimeMillis())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickModeScreen(
    products: List<Product>,
    nextInvoiceNumber: String,
    onBack: () -> Unit,
    onSaveInvoice: (Invoice) -> Long,
    onSaved: (Long) -> Unit
) {
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
            QuickSaveBar(
                total = total,
                totalPieces = totalPieces,
                linesCount = lines.size,
                enabled = lines.isNotEmpty(),
                onOpenCart = { showCart = true },
                onSave = ::saveQuickInvoice
            )
        }
    ) { padding ->
        QuickProductsTab(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            products = products,
            cartLines = lines,
            query = query,
            onQueryChange = { query = it },
            onAdd = ::addLine
        )
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

