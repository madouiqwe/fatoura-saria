package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompanySettings
import com.example.model.Invoice
import com.example.model.SaleUnit
import com.example.ui.components.InvoicePrintAdapter
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.components.formatQuantity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintPreviewScreen(
    invoice: Invoice,
    companySettings: CompanySettings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "معاينة الفاتورة للطباعة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("print_preview_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { shareInvoiceDetails(context, invoice) },
                        modifier = Modifier.testTag("print_preview_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00796B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            InvoicePrintAdapter.printInvoice(context, invoice, companySettings)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("launch_system_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("طباعة الفاتورة (Print / PDF)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("رجوع")
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFECEFF1))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A4 Styled Card exactly matching Screenshot 2
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFCFD8DC), RoundedCornerShape(8.dp))
                    .testTag("printable_invoice_canvas_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row: Company Info (Left) & Invoice Title/Details (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left: Company Info
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = companySettings.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF005B52)
                            )
                            Text(
                                text = "هاتف : ${companySettings.phone}",
                                fontSize = 12.sp,
                                color = Color(0xFF4A5568)
                            )
                            Text(
                                text = "العنوان : ${companySettings.address}",
                                fontSize = 12.sp,
                                color = Color(0xFF4A5568)
                            )
                        }

                        // Right: Invoice Title & Meta
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "فاتورة مبيعات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF005B52)
                            )
                            Text(
                                text = "رقم الفاتورة : ${invoice.invoiceNumber}",
                                fontSize = 12.sp,
                                color = Color(0xFF1A202C),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "التاريخ : ${formatDate(invoice.issueDate)}",
                                fontSize = 12.sp,
                                color = Color(0xFF4A5568)
                            )
                        }
                    }

                    // Customer & Payment Status Bar
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "العميل : ${invoice.customerName.ifBlank { "عميل نقدي" }}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1A202C)
                                )
                                Text(
                                    text = "طريقة الدفع : ${invoice.paymentMethod.labelAr}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4A5568)
                                )
                            }

                            // Green Status Badge
                            Surface(
                                color = Color(0xFF10B981),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "حالة الفاتورة: ${invoice.status.labelAr}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Invoice Items Table with Dark Teal Header (#005B52)
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Table Header
                        Surface(
                            color = Color(0xFF005B52),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الصنف",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(2.2f)
                                )
                                Text(
                                    text = "الكمية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = "السعر",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = "الإجمالي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }

                        // Table Rows
                        invoice.items.forEachIndexed { index, item ->
                            val rowBg = if (index % 2 == 0) Color.White else Color(0xFFF8FAFC)
                            Surface(
                                color = rowBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2.2f)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1A202C)
                                        )
                                        if (item.saleUnit == SaleUnit.PACKAGE) {
                                            Text(
                                                text = "${formatQuantity(item.packagesCount)} حزمة (${formatQuantity(item.unitsPerPackage)} حبة)",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (item.saleUnit == SaleUnit.PACKAGE) {
                                            "${formatQuantity(item.effectiveQuantity)}"
                                        } else {
                                            "${formatQuantity(item.quantity)}"
                                        },
                                        fontSize = 12.sp,
                                        color = Color(0xFF4A5568),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    Text(
                                        text = formatCurrency(item.unitPrice),
                                        fontSize = 12.sp,
                                        color = Color(0xFF4A5568),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    Text(
                                        text = formatCurrency(item.total),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF005B52),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.5f)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                        }
                    }

                    // Totals Section (Right Aligned)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "المجموع الفرعي : ${formatCurrency(invoice.subtotal)}",
                            fontSize = 13.sp,
                            color = Color(0xFF4A5568)
                        )

                        if (invoice.calculatedDiscount > 0) {
                            Text(
                                text = "الخصم : -${formatCurrency(invoice.calculatedDiscount)}",
                                fontSize = 13.sp,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Text(
                            text = "الإجمالي النهائي : ${formatCurrency(invoice.grandTotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF005B52)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Footer Greeting Note
                    Text(
                        text = companySettings.footerNote.ifBlank { "شكراً لتعاملكم معنا 🙏" },
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
