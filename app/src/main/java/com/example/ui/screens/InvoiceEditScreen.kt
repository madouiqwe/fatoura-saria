package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.PaymentMethod
import com.example.model.SaleUnit
import com.example.ui.InvoiceViewModel
import com.example.ui.components.EditItemDialog
import com.example.ui.components.PackageBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatQuantity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditScreen(
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val draftInvoice by viewModel.draftInvoice.collectAsStateWithLifecycle()
    val availableProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val isDraftCompletedInitially = viewModel.isDraftCompletedInitially

    val draft = draftInvoice ?: run {
        onNavigateBack()
        return
    }

    var editingItem by remember { mutableStateOf<InvoiceItem?>(null) }
    var isAddingItem by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.cancelDraft()
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "تعديل فاتورة (${draft.invoiceNumber})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (isDraftCompletedInitially || draft.status == InvoiceStatus.COMPLETED) {
                            Text(
                                text = "فاتورة مكتملة - التعديل متاح لجميع البيانات والبنود",
                                fontSize = 11.sp,
                                color = Color(0xFF137333),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.cancelDraft()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("edit_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "إلغاء")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveDraft { savedId ->
                                onSaveSuccess(savedId)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("save_invoice_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ التعديلات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
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
                        Text(
                            text = "الإجمالي النهائي:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(draft.grandTotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("draft_bottom_grand_total")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.cancelDraft()
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("cancel_edit_button")
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                viewModel.saveDraft { savedId ->
                                    onSaveSuccess(savedId)
                                }
                            },
                            modifier = Modifier.testTag("confirm_save_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Post Completion Notice Banner
            if (isDraftCompletedInitially || draft.status == InvoiceStatus.COMPLETED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ميزة التعديل بعد الإكمال مفعّلة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1E40AF)
                            )
                            Text(
                                text = "يمكنك تعديل أي سلعة، تغيير سعة الحزمة، أو التبديل بين الحزمة والقطعة بحرية تامة.",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Customer Information Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "بيانات العميل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedTextField(
                        value = draft.customerName,
                        onValueChange = {
                            viewModel.updateDraftCustomer(
                                name = it,
                                phone = draft.customerPhone,
                                email = draft.customerEmail
                            )
                        },
                        label = { Text("اسم العميل أو الشركة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_customer_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.customerPhone,
                            onValueChange = {
                                viewModel.updateDraftCustomer(
                                    name = draft.customerName,
                                    phone = it,
                                    email = draft.customerEmail
                                )
                            },
                            label = { Text("رقم الهاتف") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("edit_customer_phone_input")
                        )

                        OutlinedTextField(
                            value = draft.customerEmail,
                            onValueChange = {
                                viewModel.updateDraftCustomer(
                                    name = draft.customerName,
                                    phone = draft.customerPhone,
                                    email = it
                                )
                            },
                            label = { Text("البريد الإلكتروني") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("edit_customer_email_input")
                        )
                    }
                }
            }

            // Invoice Number Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "بيانات الفاتورة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedTextField(
                        value = draft.invoiceNumber,
                        onValueChange = { viewModel.updateDraftInvoiceNumber(it) },
                        label = { Text("رقم الفاتورة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_invoice_number_input")
                    )
                }
            }

            // Line Items Section (With variable package capacity & piece/package flexibility)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "سلع وبنود الفاتورة (${draft.items.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "يدعم سعات مختلفة للحزم (24، 10، إلخ) والبيع بالحزمة أو بالحبة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { isAddingItem = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_item_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة سلعة", fontSize = 13.sp)
                        }
                    }

                    if (draft.items.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "لا توجد سلع مسجلة في هذه الفاتورة.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { isAddingItem = true },
                                    modifier = Modifier.testTag("empty_add_item_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة أول سلعة الآن")
                                }
                            }
                        }
                    } else {
                        draft.items.forEachIndexed { index, item ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("item_row_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (item.packageInfo.isNotBlank()) {
                                                PackageBadge(packageText = item.packageInfo)
                                            }
                                        }

                                        // Sale Unit Badge & Quick Toggle
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = if (item.saleUnit == SaleUnit.PACKAGE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${item.saleUnit.iconText} بيع بـ ${item.saleUnit.labelAr} (سعة الحزمة: ${formatQuantity(item.unitsPerPackage)} حبة)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (item.saleUnit == SaleUnit.PACKAGE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            TextButton(
                                                onClick = { viewModel.toggleDraftItemSaleUnit(item.id) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier.testTag("toggle_unit_${index}")
                                            ) {
                                                Text(
                                                    text = if (item.saleUnit == SaleUnit.PACKAGE) "التحويل للبيع بالحبة" else "التحويل للبيع بالحزمة",
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        if (item.description.isNotBlank()) {
                                            Text(
                                                text = item.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Automatic Calculation Stepper Row
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Quantity / Package Stepper
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = { viewModel.decrementDraftItem(item.id) },
                                                            enabled = if (item.saleUnit == SaleUnit.PACKAGE) item.packagesCount > 1.0 else item.quantity > 1.0,
                                                            modifier = Modifier.size(28.dp).testTag("decrease_qty_${index}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "إنقاص",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                        Text(
                                                             text = if (item.saleUnit == SaleUnit.PACKAGE) {
                                                                 "${formatQuantity(item.packagesCount)} حزمة (${formatQuantity(item.effectiveQuantity)} حبة)"
                                                             } else {
                                                                 "${formatQuantity(item.quantity)} حبة"
                                                             },
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.padding(horizontal = 4.dp).testTag("item_qty_label_${index}")
                                                        )
                                                        IconButton(
                                                            onClick = { viewModel.incrementDraftItem(item.id) },
                                                            modifier = Modifier.size(28.dp).testTag("increase_qty_${index}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "زيادة",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "× ${formatCurrency(item.unitPrice)}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                Spacer(modifier = Modifier.weight(1f))

                                                // Real-time calculated line total
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "= ${formatCurrency(item.total)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).testTag("item_total_badge_${index}")
                                                    )
                                                }
                                            }

                                            // Explicit formula calculation display
                                            if (item.saleUnit == SaleUnit.PACKAGE) {
                                                Text(
                                                    text = "${formatCurrency(item.unitPrice)} (سعر الحبة) × (${formatQuantity(item.unitsPerPackage)} سعة الحزمة) × ${formatQuantity(item.packagesCount)} حزمة = ${formatCurrency(item.total)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "سعر الحزمة: ${formatCurrency(item.packagePrice)} (إجمالي: ${formatQuantity(item.effectiveQuantity)} قطعة)",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Text(
                                                    text = "${formatCurrency(item.unitPrice)} (سعر الحبة) × ${formatQuantity(item.quantity)} حبة = ${formatCurrency(item.total)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "بيع بالحبة المفردة (سعة الحزمة للمنتج: ${formatQuantity(item.unitsPerPackage)} حبة)",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { editingItem = item },
                                            modifier = Modifier.size(32.dp).testTag("edit_item_button_${index}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeDraftItem(item.id) },
                                            modifier = Modifier.size(32.dp).testTag("delete_item_button_${index}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Financial Calculation Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "تفاصيل الحساب المالي والضريبة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "المجموع الفرعي:", fontSize = 14.sp)
                        Text(
                            text = formatCurrency(draft.subtotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    HorizontalDivider()

                    // Discount Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "الخصم:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (draft.isDiscountPercentage) "نسبة مئوية (%)" else "مبلغ ثابت (د.ج)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = draft.isDiscountPercentage,
                                    onCheckedChange = { viewModel.updateDraftDiscount(draft.discountAmount, it) },
                                    modifier = Modifier.testTag("discount_type_switch")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = if (draft.discountAmount > 0) draft.discountAmount.toString() else "",
                            onValueChange = {
                                val value = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateDraftDiscount(value, draft.isDiscountPercentage)
                            },
                            label = { Text(if (draft.isDiscountPercentage) "نسبة الخصم (%)" else "قيمة الخصم (د.ج)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_discount_input")
                        )

                        if (draft.calculatedDiscount > 0) {
                            Text(
                                text = "مبلغ الخصم الفعلي: ${formatCurrency(draft.calculatedDiscount)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    HorizontalDivider()

                    // Tax Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "ضريبة القيمة المضافة:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = formatCurrency(draft.calculatedTax),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.0, 5.0, 15.0).forEach { rate ->
                                FilterChip(
                                    selected = draft.taxPercentage == rate,
                                    onClick = { viewModel.updateDraftTax(rate) },
                                    label = { Text(if (rate == 0.0) "بدون ضريبة (0%)" else "$rate%") },
                                    modifier = Modifier.weight(1f).testTag("tax_chip_${rate.toInt()}")
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Grand Total
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الإجمالي النهائي المستحق:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = formatCurrency(draft.grandTotal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("draft_grand_total_label")
                            )
                        }
                    }

                    // Payment Amount / Paid Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "المبلغ المدفوع:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            TextButton(
                                onClick = { viewModel.updateDraftPaidAmount(draft.grandTotal) },
                                modifier = Modifier.testTag("pay_full_button")
                            ) {
                                Text("سداد المبلغ بالكامل", fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = if (draft.paidAmount > 0) draft.paidAmount.toString() else "",
                            onValueChange = {
                                val value = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateDraftPaidAmount(value)
                            },
                            label = { Text("المبلغ المستلم (د.ج)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_paid_amount_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "المتبقي على العميل:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatCurrency(draft.remainingBalance),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (draft.remainingBalance > 0) Color(0xFFB06000) else Color(0xFF137333)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Payment Method Chips
                    Text(text = "طريقة الدفع:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = draft.paymentMethod == method,
                                onClick = { viewModel.updateDraftPaymentMethod(method) },
                                label = { Text(method.labelAr) },
                                modifier = Modifier.testTag("method_chip_${method.name}")
                            )
                        }
                    }
                }
            }

            // Notes and Terms Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ملاحظات وشروط",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = { viewModel.updateDraftNotes(it, draft.terms) },
                        label = { Text("ملاحظات إضافية") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("edit_notes_input")
                    )

                    OutlinedTextField(
                        value = draft.terms,
                        onValueChange = { viewModel.updateDraftNotes(draft.notes, it) },
                        label = { Text("الشروط والأحكام") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("edit_terms_input")
                    )
                }
            }
        }
    }

    // Dialog for editing an existing item (passes catalog products)
    editingItem?.let { itemToEdit ->
        EditItemDialog(
            initialItem = itemToEdit,
            availableProducts = availableProducts,
            onDismiss = { editingItem = null },
            onConfirm = { updatedItem ->
                viewModel.updateDraftItem(updatedItem)
                editingItem = null
            }
        )
    }

    // Dialog for adding a new item (passes catalog products)
    if (isAddingItem) {
        EditItemDialog(
            initialItem = null,
            availableProducts = availableProducts,
            onDismiss = { isAddingItem = false },
            onConfirm = { newItem ->
                viewModel.addDraftItem(newItem)
                isAddingItem = false
            }
        )
    }
}
