package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.InvoiceViewModel
import com.example.ui.components.CompanySettingsDialog
import com.example.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InvoiceViewModel,
    onNavigateToQuickMode: () -> Unit,
    onNavigateToFullInvoice: () -> Unit,
    onNavigateToSavedInvoices: () -> Unit,
    onNavigateToProductCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "فاتورة سريعة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00796B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.testTag("dashboard_top_bar")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8F9))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Summary Card (Exact match to Screenshot 1: Total collected in green, invoices in blue, items in teal)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_stats_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 10.dp)
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Metric 1: المبيعات المحصلة
                    Column(
                        modifier = Modifier.weight(1.3f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formatCurrency(statistics.totalCollected),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("stat_total_collected")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المبيعات المحصلة",
                            fontSize = 11.sp,
                            color = Color(0xFF757575),
                            textAlign = TextAlign.Center
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        color = Color(0xFFE0E0E0),
                        thickness = 1.dp
                    )

                    // Metric 2: فاتورة محررة
                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${statistics.totalCount}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1976D2),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("stat_invoice_count")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "فاتورة محررة",
                            fontSize = 11.sp,
                            color = Color(0xFF757575),
                            textAlign = TextAlign.Center
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        color = Color(0xFFE0E0E0),
                        thickness = 1.dp
                    )

                    // Metric 3: سلعة مسجلة
                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${allProducts.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF00796B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("stat_product_count")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "سلعة مسجلة",
                            fontSize = 11.sp,
                            color = Color(0xFF757575),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4 Main Menu Cards (Exact design & layout as in Screenshot 1)
            DashboardMenuCard(
                title = "الوضع السريع",
                subtitle = "حساب سريع، آلة حاسبة، وإيصال فوري",
                icon = Icons.Default.Bolt,
                iconBackgroundColor = Color(0xFFFFF8E1),
                iconTintColor = Color(0xFFFFA000),
                onClick = onNavigateToQuickMode,
                testTag = "menu_card_quick_mode"
            )

            DashboardMenuCard(
                title = "فاتورة كاملة",
                subtitle = "فاتورة احترافية، إكمال تلقائي، وطباعة",
                icon = Icons.Default.ReceiptLong,
                iconBackgroundColor = Color(0xFFE0F7FA),
                iconTintColor = Color(0xFF00ACC1),
                onClick = onNavigateToFullInvoice,
                testTag = "menu_card_full_invoice"
            )

            DashboardMenuCard(
                title = "الفواتير المحفوظة",
                subtitle = "عرض، بحث، تصنيفات، وإدارة الفواتير",
                icon = Icons.Default.History,
                iconBackgroundColor = Color(0xFFE3F2FD),
                iconTintColor = Color(0xFF1E88E5),
                onClick = onNavigateToSavedInvoices,
                testTag = "menu_card_saved_invoices"
            )

            DashboardMenuCard(
                title = "دليل السلع والمنتجات",
                subtitle = "إضافة وتعديل وحذف وفئات وباركود السلع",
                icon = Icons.Default.Inventory2,
                iconBackgroundColor = Color(0xFFE8F5E9),
                iconTintColor = Color(0xFF43A047),
                onClick = onNavigateToProductCatalog,
                testTag = "menu_card_product_catalog"
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(20.dp))

            // Footer (as shown in Screenshot 1: فاتورة سريعة v2.0 • نظام إدارة الفواتير والمبيعات)
            Text(
                text = "فاتورة سريعة v2.0 • نظام إدارة الفواتير والمبيعات",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
    }

    if (showSettingsDialog) {
        CompanySettingsDialog(
            currentSettings = companySettings,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                showSettingsDialog = false
                viewModel.updateCompanySettings(updated)
            }
        )
    }
}

@Composable
fun DashboardMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rounded Icon Box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Title & Subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF212121)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF757575),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
