package com.madoui.fatoura.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madoui.fatoura.ui.components.CompanySettingsDialog
import com.madoui.fatoura.ui.screens.HomeScreen
import com.madoui.fatoura.ui.screens.InvoiceDetailScreen
import com.madoui.fatoura.ui.screens.InvoiceEditScreen
import com.madoui.fatoura.ui.screens.InvoiceListScreen
import com.madoui.fatoura.ui.screens.ProductsScreen
import com.madoui.fatoura.ui.screens.QuickModeScreen

@Composable
fun FatouraApp(viewModel: FatouraViewModel) {
    val data by viewModel.data.collectAsStateWithLifecycle()

    var stack by rememberSaveable { mutableStateOf(arrayListOf("home")) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val route = stack.lastOrNull() ?: "home"

    fun push(newRoute: String) {
        stack = ArrayList(stack).apply { add(newRoute) }
    }

    fun pop() {
        if (stack.size > 1) {
            stack = ArrayList(stack.dropLast(1))
        }
    }

    fun replaceTop(newRoute: String) {
        stack = ArrayList(stack.dropLast(1)).apply { add(newRoute) }
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    when {
        route == "home" -> HomeScreen(
            data = data,
            onOpenQuick = { push("quick") },
            onOpenNewInvoice = { push("edit") },
            onOpenInvoices = { push("invoices") },
            onOpenProducts = { push("products") },
            onOpenSettings = { showSettings = true }
        )

        route == "quick" -> QuickModeScreen(
            nextInvoiceNumber = data.nextInvoiceNumber.toString(),
            onBack = { pop() },
            onSaveInvoice = viewModel::saveInvoice,
            onSaved = { id -> replaceTop("detail/$id") }
        )

        route == "products" -> ProductsScreen(
            products = data.products,
            onBack = { pop() },
            onAdd = viewModel::addProduct,
            onUpdate = viewModel::updateProduct,
            onDelete = viewModel::deleteProduct
        )

        route == "invoices" -> InvoiceListScreen(
            invoices = data.invoices,
            onBack = { pop() },
            onNew = { push("edit") },
            onOpen = { id -> push("detail/$id") }
        )

        route == "edit" || route.startsWith("edit/") -> {
            val invoiceId = route.substringAfter("/", "").toLongOrNull()
            InvoiceEditScreen(
                data = data,
                invoiceId = invoiceId,
                onBack = { pop() },
                onSaved = { id -> replaceTop("detail/$id") },
                onSaveInvoice = viewModel::saveInvoice
            )
        }

        route.startsWith("detail/") -> {
            val invoiceId = route.substringAfter("/").toLongOrNull()
            val invoice = data.invoices.firstOrNull { it.id == invoiceId }
            InvoiceDetailScreen(
                invoice = invoice,
                settings = data.settings,
                onBack = { pop() },
                onEdit = { id -> push("edit/$id") },
                onDelete = { id ->
                    viewModel.deleteInvoice(id)
                    pop()
                },
                onChangeStatus = { status ->
                    invoice?.let { viewModel.setInvoiceStatus(it.id, status) }
                }
            )
        }

        else -> HomeScreen(
            data = data,
            onOpenQuick = { push("quick") },
            onOpenNewInvoice = { push("edit") },
            onOpenInvoices = { push("invoices") },
            onOpenProducts = { push("products") },
            onOpenSettings = { showSettings = true }
        )
    }

    if (showSettings) {
        CompanySettingsDialog(
            settings = data.settings,
            onDismiss = { showSettings = false },
            onSave = { settings ->
                viewModel.updateSettings(settings)
                showSettings = false
            }
        )
    }
}
