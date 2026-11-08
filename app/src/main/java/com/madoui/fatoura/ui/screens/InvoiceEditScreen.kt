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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.madoui.fatoura.data.AppData
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceItem
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.data.PaymentMethod
import com.madoui.fatoura.data.Product
import com.madoui.fatoura.data.SaleUnit
import com.madoui.fatoura.ui.components.FatouraTopBar
import com.madoui.fatoura.ui.theme.Background
import com.madoui.fatoura.ui.theme.Blue
import com.madoui.fatoura.ui.theme.BlueSoft
import com.madoui.fatoura.ui.theme.Green
import com.madoui.fatoura.ui.theme.GreenSoft
import com.madoui.fatoura.ui.theme.LineColor
import com.madoui.fatoura.ui.theme.Teal
import com.madoui.fatoura.ui.theme.TealSoft
import com.madoui.fatoura.ui.theme.TextMuted
import com.madoui.fatoura.ui.theme.TextPrimary
import com.madoui.fatoura.util.Fmt
import java.util.concurrent.atomic.AtomicLong

private val keyGenerator = AtomicLong(System.currentTimeMillis())

private data class DraftItem(
    val key: Long,
    val productId: Long?,
    val name: String,
    val unit: SaleUnit,
    val capacity: Int,
    val piecePrice: Double,
    val quantity: Double,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
    val totalPieces: Double get() = quantity * if (unit == SaleUnit.PACKAGE) capacity.coerceAtLeast(1) else 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditScreen(
    data: AppData,
    invoiceId: Long?,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onSaveInvoice: (Invoice) -> Long
) {
    val existing = remember(invoiceId, data.invoices) {
        if (invoiceId != null) data.invoices.firstOrNull { it.id == invoiceId } else null
    }

    var customerName by remember(invoiceId) { mutableStateOf(existing?.customerName ?: "") }
    var customerPhone by remember(invoiceId) { mutableStateOf(existing?.customerPhone ?: "") }
    var payment by remember(invoiceId) { mutableStateOf(existing?.paymentMethod ?: PaymentMethod.CASH) }
    var status by remember(invoiceId) { mutableStateOf(existing?.status ?: InvoiceStatus.PAID) }
    var discountText by remember(invoiceId) {
        mutableStateOf(existing?.discount?.takeIf { it > 0.0 }?.let { Fmt.amount(it) } ?: "")
    }
    var notes by remember(invoiceId) { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val items = remember(invoiceId) {
        mutableStateListOf<DraftItem>().apply {
            existing?.items?.forEach { item ->
                add(
                    DraftItem(
                        key = if (item.id != 0L) item.id else keyGenerator.incrementAndGet(),
                        productId = item.productId,
                        name = item.name,
                        unit = item.unit,
                        capacity = item.unitsPerPackage,
                        piecePrice = if (item.unit == SaleUnit.PACKAGE) {
                            item.unitPrice / item.unitsPerPackage.coerceAtLeast(1)
                        } else item.unitPrice,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice
                    )
                )
            }
        }
    }

    var showPicker by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var editingQuantityFor by remember { mutableStateOf<DraftItem?>(null) }
    var editingPriceFor by remember { mutableStateOf<DraftItem?>(null) }

    val subtotal = items.sumOf { it.lineTotal }
    val discount = Fmt.parseNumber(discountText) ?: 0.0
    val total = (subtotal - discount).coerceAtLeast(0.0)

    fun addProduct(product: Product) {
        val unit = if (product.packageCapacity > 1) SaleUnit.PACKAGE else SaleUnit.PIECE
        val unitPrice = product.priceFor(unit)
        items.add(
            DraftItem(
                key = keyGenerator.incrementAndGet(),
                productId = product.id,
                name = product.name,
                unit = unit,
                capacity = product.packageCapacity.coerceAtLeast(1),
                piecePrice = product.pricePerPiece,
                quantity = 1.0,
                unitPrice = unitPrice
            )
        )
        showPicker = false
        error = null
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            FatouraTopBar(
                title = if (existing == null) "فاتورة جديدة" else "تعديل الفاتورة",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        if (items.isEmpty()) {
                            error = "أضف صنفاً واحداً على الأقل قبل الحفظ"
                        } else {
                            val invoice = Invoice(
                                id = existing?.id ?: 0L,
                                number = existing?.number ?: data.nextInvoiceNumber.toString(),
                                dateMillis = existing?.dateMillis ?: System.currentTimeMillis(),
                                customerName = customerName.trim(),
                                customerPhone = customerPhone.trim(),
                                paymentMethod = payment,
                                status = status,
                                items = items.map { draft ->
                                    InvoiceItem(
                                        id = draft.key,
                                        productId = draft.productId,
                                        name = draft.name,
                                        unit = draft.unit,
                                        quantity = draft.quantity,
                                        unitsPerPackage = draft.capacity,
                                        unitPrice = draft.unitPrice
                                    )
                                },
                                discount = discount,
                                notes = notes.trim()
                            )
                            onSaved(onSaveInvoice(invoice))
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "حفظ")
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 10.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("الإجمالي النهائي", fontSize = 11.sp, color = TextMuted)
                        Text(Fmt.money(total), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Teal)
                    }
                    Button(
                        onClick = {
                            if (items.isEmpty()) {
                                error = "أضف صنفاً واحداً على الأقل قبل الحفظ"
                            } else {
                                val invoice = Invoice(
                                    id = existing?.id ?: 0L,
                                    number = existing?.number ?: data.nextInvoiceNumber.toString(),
                                    dateMillis = existing?.dateMillis ?: System.currentTimeMillis(),
                                    customerName = customerName.trim(),
                                    customerPhone = customerPhone.trim(),
                                    paymentMethod = payment,
                                    status = status,
                                    items = items.map { draft ->
                                        InvoiceItem(
                                            id = draft.key,
                                            productId = draft.productId,
                                            name = draft.name,
                                            unit = draft.unit,
                                            quantity = draft.quantity,
                                            unitsPerPackage = draft.capacity,
                                            unitPrice = draft.unitPrice
                                        )
                                    },
                                    discount = discount,
                                    notes = notes.trim()
                                )
                                onSaved(onSaveInvoice(invoice))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Text("حفظ الفاتورة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = "بيانات الفاتورة", subtitle = "رقم ${existing?.number ?: data.nextInvoiceNumber}") {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("اسم العميل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("هاتف العميل (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("طريقة الدفع", fontSize = 12.sp, color = TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = payment == method,
                                onClick = { payment = method },
                                label = { Text(method.label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenSoft,
                                    selectedLabelColor = Green
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("حالة الفاتورة", fontSize = 12.sp, color = TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InvoiceStatus.entries.forEach { value ->
                            FilterChip(
                                selected = status == value,
                                onClick = { status = value },
                                label = { Text(value.label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BlueSoft,
                                    selectedLabelColor = Blue
                                )
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "أصناف الفاتورة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showPicker = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة صنف", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("لم تُضف أي صنف بعد", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(
                                "اختر منتجاً من الدليل وحدّد البيع بالحبة أو بالحزمة",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { showPicker = true }, shape = RoundedCornerShape(10.dp)) {
                                Text("إضافة صنف")
                            }
                        }
                    }
                }
            }

            items(items, key = { it.key }) { item ->
                DraftItemCard(
                    item = item,
                    onRemove = { items.removeAll { it.key == item.key } },
                    onUnitChange = { newUnit ->
                        val newPrice = if (newUnit == SaleUnit.PACKAGE) {
                            item.piecePrice * item.capacity
                        } else item.piecePrice
                        val index = items.indexOfFirst { it.key == item.key }
                        if (index >= 0) {
                            items[index] = item.copy(unit = newUnit, unitPrice = newPrice)
                        }
                    },
                    onIncrement = {
                        val index = items.indexOfFirst { it.key == item.key }
                        if (index >= 0) items[index] = item.copy(quantity = item.quantity + 1)
                    },
                    onDecrement = {
                        val index = items.indexOfFirst { it.key == item.key }
                        if (index >= 0 && item.quantity > 1) {
                            items[index] = item.copy(quantity = item.quantity - 1)
                        }
                    },
                    onEditQuantity = { editingQuantityFor = item },
                    onEditPrice = { editingPriceFor = item }
                )
            }

            item {
                SectionCard(title = "المجاميع") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المجموع الفرعي", fontSize = 13.sp, color = TextMuted)
                        Text(Fmt.money(subtotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                        label = { Text("الخصم (د.ج)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = LineColor)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الإجمالي النهائي", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(Fmt.money(total), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Teal)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            error?.let {
                item {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showPicker) {
        ProductPickerSheet(
            products = data.products,
            onDismiss = { showPicker = false },
            onPick = { addProduct(it) },
            onManual = {
                showPicker = false
                showManual = true
            }
        )
    }

    if (showManual) {
        ManualItemDialog(
            onDismiss = { showManual = false },
            onAdd = { name, piecePrice, capacity ->
                items.add(
                    DraftItem(
                        key = keyGenerator.incrementAndGet(),
                        productId = null,
                        name = name,
                        unit = if (capacity > 1) SaleUnit.PACKAGE else SaleUnit.PIECE,
                        capacity = capacity,
                        piecePrice = piecePrice,
                        quantity = 1.0,
                        unitPrice = if (capacity > 1) piecePrice * capacity else piecePrice
                    )
                )
                showManual = false
                error = null
            }
        )
    }

    editingQuantityFor?.let { item ->
        NumberInputDialog(
            title = "الكمية (${item.unit.label})",
            initial = Fmt.qty(item.quantity),
            onDismiss = { editingQuantityFor = null },
            onConfirm = { value ->
                val index = items.indexOfFirst { it.key == item.key }
                if (index >= 0) items[index] = items[index].copy(quantity = value.coerceAtLeast(0.01))
                editingQuantityFor = null
            }
        )
    }

    editingPriceFor?.let { item ->
        NumberInputDialog(
            title = "سعر ${item.unit.label} (د.ج)",
            initial = Fmt.amount(item.unitPrice),
            onDismiss = { editingPriceFor = null },
            onConfirm = { value ->
                val index = items.indexOfFirst { it.key == item.key }
                if (index >= 0) items[index] = items[index].copy(unitPrice = value.coerceAtLeast(0.0))
                editingPriceFor = null
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (subtitle != null) {
                    Spacer(Modifier.weight(1f))
                    Text(subtitle, fontSize = 11.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DraftItemCard(
    item: DraftItem,
    onRemove: () -> Unit,
    onUnitChange: (SaleUnit) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditQuantity: () -> Unit,
    onEditPrice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UnitChip(
                    text = "حبة",
                    selected = item.unit == SaleUnit.PIECE,
                    onClick = { onUnitChange(SaleUnit.PIECE) }
                )
                UnitChip(
                    text = "حزمة (${item.capacity})",
                    selected = item.unit == SaleUnit.PACKAGE,
                    onClick = { onUnitChange(SaleUnit.PACKAGE) }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${Fmt.qty(item.totalPieces)} قطعة إجمالاً",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .background(LineColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "إنقاص", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        Fmt.qty(item.quantity),
                        modifier = Modifier
                            .clickable(onClick = onEditQuantity)
                            .padding(horizontal = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onIncrement, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    "× ${Fmt.amount(item.unitPrice)}",
                    modifier = Modifier
                        .background(TealSoft, RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditPrice)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Teal
                )

                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text("الإجمالي", fontSize = 10.sp, color = TextMuted)
                    Text(Fmt.money(item.lineTotal), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            if (item.unit == SaleUnit.PACKAGE) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${Fmt.qty(item.quantity)} حزمة × ${item.capacity} حبة = ${Fmt.qty(item.totalPieces)} حبة",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun UnitChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Teal else LineColor.copy(alpha = 0.45f),
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else TextMuted
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPickerSheet(
    products: List<Product>,
    onDismiss: () -> Unit,
    onPick: (Product) -> Unit,
    onManual: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(products, query) {
        val q = query.trim()
        if (q.isEmpty()) products else products.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true) ||
                it.barcode.contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "اختر صنفاً",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onManual) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("صنف يدوي")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن منتج...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(filtered, key = { it.id }) { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(product) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(TealSoft, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(
                                "${product.packageCapacity} حبة/حزمة  •  ${Fmt.money(product.pricePerPiece)} للحبة",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Fmt.money(product.packagePrice), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Teal)
                            Text("سعر الحزمة", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                    HorizontalDivider(color = LineColor.copy(alpha = 0.6f))
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ManualItemDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, piecePrice: Double, capacity: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var capacityText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("صنف يدوي", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("اسم الصنف *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it; error = null },
                    label = { Text("سعر الحبة (د.ج) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("عدد القطع في الحزمة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
                    Button(
                        onClick = {
                            val price = Fmt.parseNumber(priceText)
                            val capacity = capacityText.toIntOrNull() ?: 1
                            when {
                                name.isBlank() -> error = "اسم الصنف مطلوب"
                                price == null || price <= 0.0 -> error = "أدخل سعراً صحيحاً"
                                else -> onAdd(name.trim(), price, capacity.coerceAtLeast(1))
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إضافة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberInputDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(12.dp)
                )
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = Fmt.parseNumber(text)
                if (value == null || value <= 0.0) error = "قيمة غير صحيحة" else onConfirm(value)
            }) {
                Text("موافق", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}
