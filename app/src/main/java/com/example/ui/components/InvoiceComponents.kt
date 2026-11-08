package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.Product
import com.example.model.SaleUnit
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val CurrencyFormatter = DecimalFormat("#,##0.00")

fun formatCurrency(amount: Double): String {
    return "${CurrencyFormatter.format(amount)} د.ج"
}

fun formatQuantity(qty: Double): String {
    return if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return sdf.format(Date(millis))
}

@Composable
fun InvoiceStatusChip(
    status: InvoiceStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon) = when (status) {
        InvoiceStatus.COMPLETED -> Triple(
            Color(0xFFE6F4EA),
            Color(0xFF137333),
            Icons.Default.CheckCircle
        )
        InvoiceStatus.PENDING -> Triple(
            Color(0xFFFEF7E0),
            Color(0xFFB06000),
            Icons.Default.HourglassEmpty
        )
        InvoiceStatus.PARTIALLY_PAID -> Triple(
            Color(0xFFE8F0FE),
            Color(0xFF1A73E8),
            Icons.Default.Payments
        )
        InvoiceStatus.CANCELLED -> Triple(
            Color(0xFFFCE8E6),
            Color(0xFFC5221F),
            Icons.Default.ErrorOutline
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status.labelAr,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PostCompletionBanner(
    isCompleted: Boolean,
    isEditedAfterCompletion: Boolean,
    modifier: Modifier = Modifier
) {
    if (isCompleted || isEditedAfterCompletion) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isEditedAfterCompletion) Color(0xFFEFF6FF) else Color(0xFFECFDF5)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isEditedAfterCompletion) Color(0xFF3B82F6) else Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditedAfterCompletion) Icons.Default.Edit else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditedAfterCompletion) "فاتورة مكتملة تم تعديلها" else "فاتورة مكتملة - قابلة للتعديل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isEditedAfterCompletion) Color(0xFF1E40AF) else Color(0xFF065F46)
                    )
                    Text(
                        text = "يمكنك تعديل أي من البنود أو المبالغ أو بيانات العميل وحفظ التحديثات في أي وقت.",
                        fontSize = 11.sp,
                        color = if (isEditedAfterCompletion) Color(0xFF1E3A8A) else Color(0xFF047857),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subValue: String? = null,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subValue != null) {
                Text(
                    text = subValue,
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPrint: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_card_${invoice.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = invoice.invoiceNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatDate(invoice.issueDate),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp).testTag("menu_button_${invoice.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("معاينة وطباعة الفاتورة") },
                            leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onPrint?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تعديل الفاتورة") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("حذف الفاتورة", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Client info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.customerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (invoice.customerPhone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = invoice.customerPhone,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                InvoiceStatusChip(status = invoice.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الإجمالي:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(invoice.grandTotal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (invoice.status == InvoiceStatus.PARTIALLY_PAID) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "المتبقي:",
                            fontSize = 11.sp,
                            color = Color(0xFFB06000)
                        )
                        Text(
                            text = formatCurrency(invoice.remainingBalance),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFFB06000)
                        )
                    }
                } else if (invoice.status == InvoiceStatus.COMPLETED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF137333),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (invoice.isEditedAfterCompletion) "مدفوعة (مُعدّلة)" else "مدفوعة بالكامل",
                            fontSize = 12.sp,
                            color = Color(0xFF137333),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(34.dp).testTag("quick_edit_button_${invoice.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (invoice.status == InvoiceStatus.COMPLETED) "تعديل الفاتورة" else "تعديل",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PackageBadge(
    packageText: String,
    modifier: Modifier = Modifier
) {
    if (packageText.isBlank()) return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = packageText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun EditItemDialog(
    initialItem: InvoiceItem?,
    availableProducts: List<Product> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (InvoiceItem) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var selectedUnit by remember { mutableStateOf(initialItem?.saleUnit ?: SaleUnit.PACKAGE) }

    // سعة الحزمة لهذا المنتج بالتحديد (افتراضياً 12 أو من الصنف)
    var unitsPerPackageText by remember {
        mutableStateOf(
            initialItem?.unitsPerPackage?.let { formatQuantity(it) } ?: "24"
        )
    }

    // عدد الحزم إذا كان البيع بالحزمة
    var packagesCountText by remember {
        mutableStateOf(
            initialItem?.packagesCount?.let { formatQuantity(it) } ?: "1"
        )
    }

    // عدد الحبات المفردة إذا كان البيع بالحبة
    var singlePiecesCountText by remember {
        mutableStateOf(
            if (initialItem != null && initialItem.saleUnit == SaleUnit.PIECE) {
                formatQuantity(initialItem.quantity)
            } else "1"
        )
    }

    var packageTypeName by remember {
        mutableStateOf(
            initialItem?.packageInfo?.ifBlank { "كرتونة" } ?: "كرتونة"
        )
    }

    var priceText by remember {
        mutableStateOf(initialItem?.unitPrice?.let { if (it > 0) it.toString() else "" } ?: "")
    }

    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val units = unitsPerPackageText.toDoubleOrNull() ?: 1.0
    val packages = packagesCountText.toDoubleOrNull() ?: 0.0
    val pieces = singlePiecesCountText.toDoubleOrNull() ?: 0.0
    val price = priceText.toDoubleOrNull() ?: 0.0

    // حساب عدد القطع الإجمالي والإجمالي المالي
    val totalQty = if (selectedUnit == SaleUnit.PACKAGE) {
        (units * packages).coerceAtLeast(0.0)
    } else {
        pieces.coerceAtLeast(0.0)
    }

    val totalAmount = if (selectedUnit == SaleUnit.PACKAGE) {
        (units * packages * price).coerceAtLeast(0.0)
    } else {
        (pieces * price).coerceAtLeast(0.0)
    }

    val singlePackagePrice = (units * price).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem == null) "إضافة صنف للفاتورة" else "تعديل الصنف",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Catalog Selection Chips (إذا توفرت منتجات في الدليل)
                if (availableProducts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "اختر من دليل المنتجات (تعبئة تلقائية لسعة الحزمة والسعر):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableProducts.take(6).forEach { product ->
                                FilterChip(
                                    selected = (name == product.name),
                                    onClick = {
                                        name = product.name
                                        unitsPerPackageText = formatQuantity(product.unitsPerPackage)
                                        priceText = if (product.defaultUnitPrice > 0) product.defaultUnitPrice.toString() else priceText
                                        packageTypeName = product.packageTypeName
                                    },
                                    label = {
                                        Text(
                                            text = "${product.name} (${formatQuantity(product.unitsPerPackage)} حبة)",
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف / المنتج *") },
                    placeholder = { Text("مثال: مرساي كتان أو عصير برتقال") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("item_name_input")
                )

                // Package Capacity Section (كم حبة في الحزمة؟)
                Surface(
                    color = Color(0xFFE0F2F1),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📦 كم حبة في الحزمة الواحدة؟ (سعة الحزمة):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF004D40)
                            )
                            Surface(
                                color = Color(0xFF00796B),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${formatQuantity(units)} حبة بالحزمة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Quick Preset Chips for capacity
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("6", "10", "12", "15", "20", "24", "30", "40").forEach { cap ->
                                FilterChip(
                                    selected = (unitsPerPackageText == cap),
                                    onClick = { unitsPerPackageText = cap },
                                    label = { Text("$cap حبة", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00796B),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = unitsPerPackageText,
                            onValueChange = { unitsPerPackageText = it },
                            label = { Text("سعة الحزمة (عدد القطع في كل حزمة/كرتونة) *") },
                            placeholder = { Text("مثلاً: 10 أو 24") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("item_units_per_package_input")
                        )
                    }
                }

                // Pricing Row (سعر الحبة وسعر الحزمة)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("سعر الحبة المفردة (د.ج) *") },
                        placeholder = { Text("مثلاً: 420") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("item_price_input")
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "سعر الحزمة بالكامل:",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(singlePackagePrice),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF00796B)
                            )
                            Text(
                                text = "(${formatQuantity(units)} حبة × ${formatCurrency(price)})",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Sale Unit Toggle: حزمة أم حبة مفردة
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "وحدة البيع في هذه الفاتورة:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = (selectedUnit == SaleUnit.PACKAGE),
                                onClick = { selectedUnit = SaleUnit.PACKAGE },
                                label = {
                                    Text(
                                        text = "📦 بيع بالحزمة (${packageTypeName.ifBlank { "حزمة" }})",
                                        fontWeight = if (selectedUnit == SaleUnit.PACKAGE) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("unit_package_chip")
                            )

                            FilterChip(
                                selected = (selectedUnit == SaleUnit.PIECE),
                                onClick = { selectedUnit = SaleUnit.PIECE },
                                label = {
                                    Text(
                                        text = "🏷️ بيع بالحبة المفردة",
                                        fontWeight = if (selectedUnit == SaleUnit.PIECE) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("unit_piece_chip")
                            )
                        }

                        if (selectedUnit == SaleUnit.PACKAGE) {
                            // Package count input + type name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = packagesCountText,
                                    onValueChange = { packagesCountText = it },
                                    label = { Text("عدد الحزم") },
                                    placeholder = { Text("مثلاً: 3") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("item_packages_count_input")
                                )

                                OutlinedTextField(
                                    value = packageTypeName,
                                    onValueChange = { packageTypeName = it },
                                    label = { Text("نوع الحزمة") },
                                    placeholder = { Text("كرتونة، شد، صندوق") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("item_package_type_input")
                                )
                            }

                            // Package price breakdown
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سعر الحزمة الواحدة (${formatQuantity(units)} حبة):",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${formatCurrency(price)} × ${formatQuantity(units)} = ${formatCurrency(singlePackagePrice)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            // Piece count input
                            OutlinedTextField(
                                value = singlePiecesCountText,
                                onValueChange = { singlePiecesCountText = it },
                                label = { Text("عدد الحبات المفردة") },
                                placeholder = { Text("مثلاً: 5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("item_pieces_count_input")
                            )

                            Text(
                                text = "💡 سعة الحزمة المسجلة لهذا المنتج هي (${formatQuantity(units)} حبة)، ويتم الحساب بالقطعة المفردة.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("ملاحظات / وصف إضافي (اختياري)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("item_description_input")
                )

                // Prominent Live Calculation Result Box
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("live_item_calculation_box")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "الإجمالي التلقائي للبند:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = formatCurrency(totalAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (selectedUnit == SaleUnit.PACKAGE) {
                            Text(
                                text = "${formatCurrency(price)} (سعر الحبة) × (${formatQuantity(units)} سعة الحزمة) × ${formatQuantity(packages)} حزم = ${formatCurrency(totalAmount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "إجمالي عدد القطع الفعلي: ${formatQuantity(totalQty)} قطعة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "${formatCurrency(price)} (سعر الحبة) × ${formatQuantity(pieces)} حبات = ${formatCurrency(totalAmount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "البيع بالحبة المفردة (سعة الحزمة للمنتج: ${formatQuantity(units)} حبة)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "يرجى كتابة اسم الصنف"
                        return@Button
                    }
                    val validUnits = unitsPerPackageText.toDoubleOrNull() ?: 1.0
                    val validPrice = priceText.toDoubleOrNull()
                    if (validPrice == null || validPrice < 0) {
                        errorMessage = "يرجى إدخال سعر صحيح"
                        return@Button
                    }

                    val validPacks = packagesCountText.toDoubleOrNull() ?: 1.0
                    val validPieces = singlePiecesCountText.toDoubleOrNull() ?: 1.0

                    val packageBadgeStr = if (selectedUnit == SaleUnit.PACKAGE) {
                        "${packageTypeName.ifBlank { "حزمة" }} (${formatQuantity(validUnits)} حبة)"
                    } else {
                        "حبة مفردة (سعة الحزمة ${formatQuantity(validUnits)})"
                    }

                    val finalItem = InvoiceItem(
                        id = initialItem?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        packageInfo = packageBadgeStr,
                        unitsPerPackage = validUnits,
                        packagesCount = if (selectedUnit == SaleUnit.PACKAGE) validPacks else 0.0,
                        singlePiecesCount = if (selectedUnit == SaleUnit.PIECE) validPieces else 0.0,
                        saleUnit = selectedUnit,
                        quantity = if (selectedUnit == SaleUnit.PACKAGE) (validUnits * validPacks) else validPieces,
                        unitPrice = validPrice,
                        description = description.trim()
                    )
                    onConfirm(finalItem)
                },
                modifier = Modifier.testTag("confirm_item_button")
            ) {
                Text("تأكيد وحفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
