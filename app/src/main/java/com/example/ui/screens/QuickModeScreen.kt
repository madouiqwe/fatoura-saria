package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.InvoiceItem
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.SaleUnit
import com.example.ui.InvoiceViewModel
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatQuantity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickModeScreen(
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit,
    onInvoiceCreated: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    var customerName by remember { mutableStateOf("عميل نقدي") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    val quickItems = remember { mutableStateListOf<InvoiceItem>() }

    // Custom quick item inputs
    var customItemName by remember { mutableStateOf("") }
    var customItemPrice by remember { mutableStateOf("") }
    var customItemCapacity by remember { mutableStateOf("10") }

    val totalAmount = quickItems.sumOf { it.total }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF8E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "الوضع السريع • إيصال فوري",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("quick_mode_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00796B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "إجمالي الإيصال:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = formatCurrency(totalAmount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color(0xFF00796B),
                            modifier = Modifier.testTag("quick_mode_grand_total")
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveQuickSale(
                                customerName = customerName,
                                items = quickItems.toList(),
                                paymentMethod = selectedPaymentMethod,
                                onSuccess = { savedId ->
                                    onInvoiceCreated(savedId)
                                }
                            )
                        },
                        enabled = quickItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("quick_mode_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إصدار الإيصال وطباعته", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Customer & Payment Info Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("اسم العميل (سريع)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("quick_customer_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                PaymentMethod.CASH to "نقداً",
                                PaymentMethod.BANK_TRANSFER to "تحويل بنكي",
                                PaymentMethod.CHEQUE to "شيك"
                            ).forEach { (method, label) ->
                                FilterChip(
                                    selected = selectedPaymentMethod == method,
                                    onClick = { selectedPaymentMethod = method },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE0F2F1),
                                        selectedLabelColor = Color(0xFF004D40)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Product Selection Chips from Catalog
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "اختر سلعة من الدليل لإضافتها فوراً:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF00796B)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allProducts.forEach { product ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        // Add to quick items
                                        quickItems.add(
                                            InvoiceItem(
                                                id = UUID.randomUUID().toString(),
                                                name = product.name,
                                                packageInfo = "${product.packageTypeName} (${formatQuantity(product.unitsPerPackage)} حبة)",
                                                unitsPerPackage = product.unitsPerPackage,
                                                packagesCount = 1.0,
                                                singlePiecesCount = 0.0,
                                                saleUnit = SaleUnit.PACKAGE,
                                                quantity = product.unitsPerPackage,
                                                unitPrice = product.defaultUnitPrice,
                                                description = product.description
                                            )
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "+ ${product.name} (${formatCurrency(product.defaultUnitPrice)})",
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Custom Item Input Row
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "أو أدخل سلعة وحساب سريع مخصص:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customItemName,
                                onValueChange = { customItemName = it },
                                label = { Text("اسم الصنف") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                            OutlinedTextField(
                                value = customItemPrice,
                                onValueChange = { customItemPrice = it },
                                label = { Text("السعر (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customItemCapacity,
                                onValueChange = { customItemCapacity = it },
                                label = { Text("كم حبة بالحزمة؟") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1.1f)
                            )
                        }

                        Button(
                            onClick = {
                                val price = customItemPrice.toDoubleOrNull() ?: 0.0
                                val capacity = customItemCapacity.toDoubleOrNull() ?: 10.0
                                if (customItemName.isNotBlank() && price > 0) {
                                    quickItems.add(
                                        InvoiceItem(
                                            id = UUID.randomUUID().toString(),
                                            name = customItemName.trim(),
                                            packageInfo = "حزمة (${formatQuantity(capacity)} حبة)",
                                            unitsPerPackage = capacity,
                                            packagesCount = 1.0,
                                            singlePiecesCount = 0.0,
                                            saleUnit = SaleUnit.PACKAGE,
                                            quantity = capacity,
                                            unitPrice = price
                                        )
                                    )
                                    customItemName = ""
                                    customItemPrice = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة للإيصال")
                        }
                    }
                }
            }

            // Items in the current quick receipt
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بنود الإيصال السريع (${quickItems.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (quickItems.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { quickItems.clear() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("مسح الكل", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (quickItems.isEmpty()) {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "لم تتم إضافة سلع بعد. اختر من الدليل بالأعلى أو اكتب صنف وسعر.",
                            fontSize = 13.sp,
                            color = Color(0xFF757575),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            } else {
                items(quickItems, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))

                                // Toggle unit (حزمة أم حبة)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = if (item.saleUnit == SaleUnit.PACKAGE) Color(0xFFE0F2F1) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (item.saleUnit == SaleUnit.PACKAGE) {
                                                "📦 ${formatQuantity(item.packagesCount)} حزمة (سعة ${formatQuantity(item.unitsPerPackage)}) = ${formatQuantity(item.effectiveQuantity)} حبة"
                                            } else {
                                                "🏷️ ${formatQuantity(item.quantity)} حبة مفردة"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (item.saleUnit == SaleUnit.PACKAGE) Color(0xFF004D40) else Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val index = quickItems.indexOf(item)
                                            if (index >= 0) {
                                                val toggled = if (item.saleUnit == SaleUnit.PACKAGE) {
                                                    item.copy(
                                                        saleUnit = SaleUnit.PIECE,
                                                        singlePiecesCount = item.effectiveQuantity,
                                                        quantity = item.effectiveQuantity
                                                    )
                                                } else {
                                                    val packs = (item.quantity / item.unitsPerPackage).coerceAtLeast(1.0)
                                                    item.copy(
                                                        saleUnit = SaleUnit.PACKAGE,
                                                        packagesCount = packs.toInt().toDouble().coerceAtLeast(1.0),
                                                        quantity = packs.toInt() * item.unitsPerPackage
                                                    )
                                                }
                                                quickItems[index] = toggled
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(
                                            text = if (item.saleUnit == SaleUnit.PACKAGE) "تحويل لحبة" else "تحويل لحزمة",
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatCurrency(item.unitPrice)} × ${if (item.saleUnit == SaleUnit.PACKAGE) "${formatQuantity(item.effectiveQuantity)} حبة" else "${formatQuantity(item.quantity)} حبة"} = ${formatCurrency(item.total)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00796B),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Stepper Controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val index = quickItems.indexOf(item)
                                        if (index >= 0) {
                                            if (item.saleUnit == SaleUnit.PACKAGE) {
                                                if (item.packagesCount > 1.0) {
                                                    val p = item.packagesCount - 1.0
                                                    quickItems[index] = item.copy(packagesCount = p, quantity = p * item.unitsPerPackage)
                                                }
                                            } else {
                                                if (item.quantity > 1.0) {
                                                    val q = item.quantity - 1.0
                                                    quickItems[index] = item.copy(singlePiecesCount = q, quantity = q)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = if (item.saleUnit == SaleUnit.PACKAGE) "${formatQuantity(item.packagesCount)}" else "${formatQuantity(item.quantity)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val index = quickItems.indexOf(item)
                                        if (index >= 0) {
                                            if (item.saleUnit == SaleUnit.PACKAGE) {
                                                val p = item.packagesCount + 1.0
                                                quickItems[index] = item.copy(packagesCount = p, quantity = p * item.unitsPerPackage)
                                            } else {
                                                val q = item.quantity + 1.0
                                                quickItems[index] = item.copy(singlePiecesCount = q, quantity = q)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { quickItems.remove(item) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
