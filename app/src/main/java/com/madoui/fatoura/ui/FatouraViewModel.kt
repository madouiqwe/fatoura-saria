package com.madoui.fatoura.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.madoui.fatoura.data.AppData
import com.madoui.fatoura.data.CompanySettings
import com.madoui.fatoura.data.FatouraRepository
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.data.Product
import kotlinx.coroutines.flow.StateFlow

class FatouraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FatouraRepository(application)

    val data: StateFlow<AppData> = repository.data

    fun addProduct(product: Product) = repository.addProduct(product)

    fun updateProduct(product: Product) = repository.updateProduct(product)

    fun deleteProduct(productId: Long) = repository.deleteProduct(productId)

    fun saveInvoice(invoice: Invoice): Long = repository.saveInvoice(invoice)

    fun deleteInvoice(invoiceId: Long) = repository.deleteInvoice(invoiceId)

    fun setInvoiceStatus(invoiceId: Long, status: InvoiceStatus) =
        repository.setInvoiceStatus(invoiceId, status)

    fun updateSettings(settings: CompanySettings) = repository.updateSettings(settings)
}
