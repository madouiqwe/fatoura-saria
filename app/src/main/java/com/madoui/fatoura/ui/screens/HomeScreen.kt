package com.madoui.fatoura.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madoui.fatoura.data.AppData
import com.madoui.fatoura.ui.components.FatouraTopBar
import com.madoui.fatoura.ui.theme.Amber
import com.madoui.fatoura.ui.theme.AmberSoft
import com.madoui.fatoura.ui.theme.Blue
import com.madoui.fatoura.ui.theme.BlueSoft
import com.madoui.fatoura.ui.theme.Green
import com.madoui.fatoura.ui.theme.GreenSoft
import com.madoui.fatoura.ui.theme.LineColor
import com.madoui.fatoura.ui.theme.StatBackground
import com.madoui.fatoura.ui.theme.Teal
import com.madoui.fatoura.ui.theme.TealSoft
import com.madoui.fatoura.ui.theme.TextMuted
import com.madoui.fatoura.ui.theme.TextPrimary
import com.madoui.fatoura.util.Fmt

@Composable
fun HomeScreen(
    data: AppData,
    onOpenQuick: () -> Unit,
    onOpenNewInvoice: () -> Unit,
    onOpenInvoices: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            FatouraTopBar(
                title = "فاتورة سريعة",
                centered = true,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            StatsCard(
                sales = data.collectedSales,
                invoiceCount = data.invoices.size,
                productCount = data.products.size,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            NavCard(
                icon = Icons.Filled.Bolt,
                iconTint = Amber,
                iconBackground = AmberSoft,
                title = "الوضع السريع",
                subtitle = "اختر السلع، احسب الإجمالي، وأصدر فاتورة فوراً",
                onClick = onOpenQuick
            )
            NavCard(
                icon = Icons.Filled.ReceiptLong,
                iconTint = Teal,
                iconBackground = TealSoft,
                title = "فاتورة كاملة",
                subtitle = "فاتورة احترافية، إكمال تلقائي، وطباعة",
                onClick = onOpenNewInvoice
            )
            NavCard(
                icon = Icons.Filled.History,
                iconTint = Blue,
                iconBackground = BlueSoft,
                title = "الفواتير المحفوظة",
                subtitle = "عرض، بحث، تصنيفات، وإدارة الفواتير",
                onClick = onOpenInvoices
            )
            NavCard(
                icon = Icons.Filled.Inventory2,
                iconTint = Green,
                iconBackground = GreenSoft,
                title = "دليل السلع والمنتجات",
                subtitle = "إضافة وتعديل وحذف وفئات وباركود السلع",
                onClick = onOpenProducts
            )

            Spacer(Modifier.height(28.dp))
            Text(
                text = "فاتورة سريعة v2.0  •  نظام إدارة الفواتير والمبيعات",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatsCard(
    sales: Double,
    invoiceCount: Int,
    productCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StatBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(
                value = Fmt.money(sales),
                label = "المبيعات المحصلة",
                valueColor = Green,
                modifier = Modifier.weight(1.35f)
            )
            StatDivider()
            StatColumn(
                value = invoiceCount.toString(),
                label = "فاتورة محررة",
                valueColor = Blue,
                modifier = Modifier.weight(0.8f)
            )
            StatDivider()
            StatColumn(
                value = productCount.toString(),
                label = "سلعة مسجلة",
                valueColor = Teal,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .height(38.dp)
            .width(1.dp)
            .background(LineColor)
    )
}

@Composable
private fun NavCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LineColor.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(iconBackground, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
            }

            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.75f)
            )
        }
    }
}
