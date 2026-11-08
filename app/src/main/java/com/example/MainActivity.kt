package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.InvoiceViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InvoiceDetailScreen
import com.example.ui.screens.InvoiceEditScreen
import com.example.ui.screens.PrintPreviewScreen
import com.example.ui.screens.ProductCatalogScreen
import com.example.ui.screens.QuickModeScreen
import com.example.ui.theme.MyApplicationTheme

sealed interface Screen {
    data object Dashboard : Screen
    data object QuickMode : Screen
    data object SavedInvoices : Screen
    data object ProductCatalog : Screen
    data class Detail(val invoiceId: Long) : Screen
    data object Edit : Screen
    data class PrintPreview(val invoiceId: Long) : Screen
}

class MainActivity : ComponentActivity() {

    private val viewModel: InvoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InvoiceApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun InvoiceApp(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()

    when (val screen = currentScreen) {
        is Screen.Dashboard -> {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToQuickMode = {
                    currentScreen = Screen.QuickMode
                },
                onNavigateToFullInvoice = {
                    viewModel.startNewInvoice()
                    currentScreen = Screen.Edit
                },
                onNavigateToSavedInvoices = {
                    currentScreen = Screen.SavedInvoices
                },
                onNavigateToProductCatalog = {
                    currentScreen = Screen.ProductCatalog
                },
                modifier = modifier
            )
        }

        is Screen.QuickMode -> {
            QuickModeScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                onInvoiceCreated = { savedId ->
                    currentScreen = Screen.PrintPreview(savedId)
                },
                modifier = modifier
            )
        }

        is Screen.SavedInvoices -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                onNavigateToDetail = { id ->
                    currentScreen = Screen.Detail(id)
                },
                onNavigateToEdit = { invoice ->
                    viewModel.startEditingInvoice(invoice)
                    currentScreen = Screen.Edit
                },
                onNavigateToPrint = { id ->
                    currentScreen = Screen.PrintPreview(id)
                },
                modifier = modifier
            )
        }

        is Screen.ProductCatalog -> {
            ProductCatalogScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                modifier = modifier
            )
        }

        is Screen.Detail -> {
            val invoice = allInvoices.find { it.id == screen.invoiceId }
            if (invoice != null) {
                InvoiceDetailScreen(
                    invoice = invoice,
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.SavedInvoices },
                    onEdit = { inv ->
                        viewModel.startEditingInvoice(inv)
                        currentScreen = Screen.Edit
                    },
                    onNavigateToPrint = { id ->
                        currentScreen = Screen.PrintPreview(id)
                    },
                    modifier = modifier
                )
            } else {
                currentScreen = Screen.SavedInvoices
            }
        }

        is Screen.Edit -> {
            InvoiceEditScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Dashboard },
                onSaveSuccess = { savedId ->
                    currentScreen = Screen.PrintPreview(savedId)
                },
                modifier = modifier
            )
        }

        is Screen.PrintPreview -> {
            val invoice = allInvoices.find { it.id == screen.invoiceId }
            if (invoice != null) {
                PrintPreviewScreen(
                    invoice = invoice,
                    companySettings = companySettings,
                    onBack = { currentScreen = Screen.Dashboard },
                    modifier = modifier
                )
            } else {
                currentScreen = Screen.Dashboard
            }
        }
    }
}
