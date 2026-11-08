package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.InvoiceRepository
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.SaleUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class InvoiceStatistics(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val partialCount: Int = 0,
    val totalAmount: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalRemaining: Double = 0.0
)

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InvoiceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = InvoiceRepository(database.invoiceDao(), database.productDao())
        viewModelScope.launch {
            repository.populateSampleDataIfEmpty()
        }
    }

    val allInvoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<InvoiceStatus?>(null)
    val selectedStatusFilter: StateFlow<InvoiceStatus?> = _selectedStatusFilter.asStateFlow()

    val filteredInvoices: StateFlow<List<Invoice>> = combine(
        allInvoices,
        _searchQuery,
        _selectedStatusFilter
    ) { invoices, query, filter ->
        invoices.filter { invoice ->
            val matchesQuery = query.isBlank() ||
                    invoice.customerName.contains(query, ignoreCase = true) ||
                    invoice.invoiceNumber.contains(query, ignoreCase = true) ||
                    invoice.items.any { it.name.contains(query, ignoreCase = true) }

            val matchesFilter = filter == null || invoice.status == filter

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val statistics: StateFlow<InvoiceStatistics> = allInvoices.combine(_searchQuery) { invoices, _ ->
        var totalAmount = 0.0
        var totalCollected = 0.0
        var totalRemaining = 0.0
        var completed = 0
        var pending = 0
        var partial = 0

        for (inv in invoices) {
            totalAmount += inv.grandTotal
            totalCollected += inv.paidAmount
            totalRemaining += inv.remainingBalance
            when (inv.status) {
                InvoiceStatus.COMPLETED -> completed++
                InvoiceStatus.PENDING -> pending++
                InvoiceStatus.PARTIALLY_PAID -> partial++
                InvoiceStatus.CANCELLED -> {}
            }
        }

        InvoiceStatistics(
            totalCount = invoices.size,
            completedCount = completed,
            pendingCount = pending,
            partialCount = partial,
            totalAmount = totalAmount,
            totalCollected = totalCollected,
            totalRemaining = totalRemaining
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvoiceStatistics()
    )

    // Current invoice being edited / created
    private val _draftInvoice = MutableStateFlow<Invoice?>(null)
    val draftInvoice: StateFlow<Invoice?> = _draftInvoice.asStateFlow()

    // Flag indicating whether the invoice being edited was previously completed
    val isDraftCompletedInitially: Boolean
        get() = _initialCompletionState

    private var _initialCompletionState: Boolean = false

    // User message/toast
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: InvoiceStatus?) {
        _selectedStatusFilter.value = filter
    }

    fun startNewInvoice() {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyMMdd-HHmm", Locale.US)
        val defaultNumber = "INV-${dateFormat.format(Date(now))}"

        _initialCompletionState = false
        _draftInvoice.value = Invoice(
            id = 0L,
            invoiceNumber = defaultNumber,
            customerName = "",
            customerPhone = "",
            customerEmail = "",
            issueDate = now,
            dueDate = now + (7L * 24 * 60 * 60 * 1000),
            status = InvoiceStatus.PENDING,
            paymentMethod = PaymentMethod.CASH,
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "عصير برتقال طبيعي 250 مل",
                    packageInfo = "كرتونة (24 حبة)",
                    unitsPerPackage = 24.0,
                    packagesCount = 2.0,
                    singlePiecesCount = 0.0,
                    saleUnit = SaleUnit.PACKAGE,
                    quantity = 48.0,
                    unitPrice = 2.50,
                    description = "كرتونة 24 حبة"
                )
            ),
            discountAmount = 0.0,
            isDiscountPercentage = false,
            taxPercentage = 15.0,
            paidAmount = 0.0,
            notes = "",
            terms = "شكراً لتعاملكم معنا. الدفع خلال مدة الاستحقاق المحددة.",
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            isEditedAfterCompletion = false
        )
    }

    fun startEditingInvoice(invoice: Invoice) {
        _initialCompletionState = invoice.status == InvoiceStatus.COMPLETED || invoice.completedAt != null
        _draftInvoice.value = invoice.copy()
    }

    fun cancelDraft() {
        _draftInvoice.value = null
        _initialCompletionState = false
    }

    fun updateDraftCustomer(name: String, phone: String, email: String) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            customerName = name,
            customerPhone = phone,
            customerEmail = email
        )
    }

    fun updateDraftInvoiceNumber(number: String) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            invoiceNumber = number
        )
    }

    fun updateDraftDates(issueDate: Long, dueDate: Long) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            issueDate = issueDate,
            dueDate = dueDate
        )
    }

    fun updateDraftStatus(status: InvoiceStatus, paidAmount: Double? = null) {
        val current = _draftInvoice.value ?: return
        val newPaid = paidAmount ?: when (status) {
            InvoiceStatus.COMPLETED -> current.grandTotal
            InvoiceStatus.PENDING -> 0.0
            InvoiceStatus.CANCELLED -> 0.0
            InvoiceStatus.PARTIALLY_PAID -> if (current.paidAmount > 0) current.paidAmount else (current.grandTotal / 2.0)
        }
        _draftInvoice.value = current.copy(
            status = status,
            paidAmount = newPaid
        )
    }

    fun updateDraftPaidAmount(amount: Double) {
        val current = _draftInvoice.value ?: return
        val total = current.grandTotal
        val status = when {
            amount >= total && total > 0 -> InvoiceStatus.COMPLETED
            amount > 0 -> InvoiceStatus.PARTIALLY_PAID
            else -> InvoiceStatus.PENDING
        }
        _draftInvoice.value = current.copy(
            paidAmount = amount,
            status = status
        )
    }

    fun updateDraftPaymentMethod(method: PaymentMethod) {
        _draftInvoice.value = _draftInvoice.value?.copy(paymentMethod = method)
    }

    fun updateDraftDiscount(amount: Double, isPercentage: Boolean) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            discountAmount = amount,
            isDiscountPercentage = isPercentage
        )
    }

    fun updateDraftTax(percentage: Double) {
        _draftInvoice.value = _draftInvoice.value?.copy(taxPercentage = percentage)
    }

    fun updateDraftNotes(notes: String, terms: String) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            notes = notes,
            terms = terms
        )
    }

    private fun updateDraftItems(newItems: List<InvoiceItem>) {
        val current = _draftInvoice.value ?: return
        val updatedInvoice = current.copy(
            items = newItems,
            updatedAt = System.currentTimeMillis()
        )
        val finalInvoice = if (updatedInvoice.status == InvoiceStatus.COMPLETED && _initialCompletionState) {
            updatedInvoice.copy(paidAmount = updatedInvoice.grandTotal)
        } else {
            updatedInvoice
        }
        _draftInvoice.value = finalInvoice
    }

    fun addDraftItem(item: InvoiceItem) {
        val current = _draftInvoice.value ?: return
        updateDraftItems(current.items + item)
    }

    fun updateDraftItem(item: InvoiceItem) {
        val current = _draftInvoice.value ?: return
        val updated = current.items.map { if (it.id == item.id) item else it }
        updateDraftItems(updated)
    }

    /**
     * Toggles the item's sale unit between PACKAGE and PIECE
     * and adjusts the counts cleanly.
     */
    fun toggleDraftItemSaleUnit(itemId: String) {
        val current = _draftInvoice.value ?: return
        val updated = current.items.map { item ->
            if (item.id == itemId) {
                if (item.saleUnit == SaleUnit.PACKAGE) {
                    // Switch to Piece: set pieces count to the current quantity or 1
                    val pieces = if (item.quantity > 0) item.quantity else 1.0
                    item.copy(
                        saleUnit = SaleUnit.PIECE,
                        singlePiecesCount = pieces,
                        quantity = pieces
                    )
                } else {
                    // Switch to Package: package count = ceil(quantity / unitsPerPackage)
                    val packs = (item.quantity / item.unitsPerPackage).coerceAtLeast(1.0)
                    val effectivePacks = packs.toInt().toDouble().coerceAtLeast(1.0)
                    item.copy(
                        saleUnit = SaleUnit.PACKAGE,
                        packagesCount = effectivePacks,
                        quantity = effectivePacks * item.unitsPerPackage
                    )
                }
            } else item
        }
        updateDraftItems(updated)
    }

    /**
     * Increments quantity or package count depending on the item's sale unit
     */
    fun incrementDraftItem(itemId: String, step: Double = 1.0) {
        val current = _draftInvoice.value ?: return
        val target = current.items.find { it.id == itemId } ?: return
        if (target.saleUnit == SaleUnit.PACKAGE) {
            val newPacks = target.packagesCount + step
            val newQty = newPacks * target.unitsPerPackage
            val updated = current.items.map {
                if (it.id == itemId) it.copy(packagesCount = newPacks, quantity = newQty) else it
            }
            updateDraftItems(updated)
        } else {
            val newPieces = target.quantity + step
            val updated = current.items.map {
                if (it.id == itemId) it.copy(singlePiecesCount = newPieces, quantity = newPieces) else it
            }
            updateDraftItems(updated)
        }
    }

    /**
     * Decrements quantity or package count depending on the item's sale unit
     */
    fun decrementDraftItem(itemId: String, step: Double = 1.0) {
        val current = _draftInvoice.value ?: return
        val target = current.items.find { it.id == itemId } ?: return
        if (target.saleUnit == SaleUnit.PACKAGE) {
            val newPacks = (target.packagesCount - step).coerceAtLeast(1.0)
            val newQty = newPacks * target.unitsPerPackage
            val updated = current.items.map {
                if (it.id == itemId) it.copy(packagesCount = newPacks, quantity = newQty) else it
            }
            updateDraftItems(updated)
        } else {
            val newPieces = (target.quantity - step).coerceAtLeast(1.0)
            val updated = current.items.map {
                if (it.id == itemId) it.copy(singlePiecesCount = newPieces, quantity = newPieces) else it
            }
            updateDraftItems(updated)
        }
    }

    /**
     * Updates an item's package capacity (units per package) and recalculates
     * total pieces and total price immediately.
     */
    fun updateDraftItemCapacity(itemId: String, newCapacity: Double) {
        val current = _draftInvoice.value ?: return
        val validCapacity = newCapacity.coerceAtLeast(1.0)
        val updated = current.items.map { item ->
            if (item.id == itemId) {
                val newQty = if (item.saleUnit == SaleUnit.PACKAGE) {
                    item.packagesCount * validCapacity
                } else {
                    item.quantity
                }
                item.copy(
                    unitsPerPackage = validCapacity,
                    quantity = newQty
                )
            } else item
        }
        updateDraftItems(updated)
    }

    /**
     * Updates unit price and recalculates line total and invoice totals immediately.
     */
    fun updateDraftItemUnitPrice(itemId: String, unitPrice: Double) {
        val current = _draftInvoice.value ?: return
        val validPrice = unitPrice.coerceAtLeast(0.0)
        val updated = current.items.map {
            if (it.id == itemId) it.copy(unitPrice = validPrice) else it
        }
        updateDraftItems(updated)
    }

    fun removeDraftItem(itemId: String) {
        val current = _draftInvoice.value ?: return
        updateDraftItems(current.items.filterNot { it.id == itemId })
    }

    fun saveDraft(onSaved: (Long) -> Unit) {
        val current = _draftInvoice.value ?: return
        if (current.customerName.isBlank()) {
            _userMessage.value = "يرجى كتابة اسم العميل"
            return
        }
        if (current.items.isEmpty()) {
            _userMessage.value = "يرجى إضافة بند واحد على الأقل في الفاتورة"
            return
        }

        viewModelScope.launch {
            val wasInitiallyCompleted = _initialCompletionState
            val savedId = repository.saveInvoice(current)
            _draftInvoice.value = null
            _initialCompletionState = false

            if (wasInitiallyCompleted) {
                _userMessage.value = "تم تعديل وحفظ الفاتورة المكتملة بنجاح ✅"
            } else {
                _userMessage.value = "تم حفظ الفاتورة بنجاح ✅"
            }
            onSaved(savedId)
        }
    }

    fun duplicateInvoice(id: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateInvoice(id)
            if (newId != null) {
                _userMessage.value = "تم تكرار الفاتورة كنسخة جديدة بنجاح"
            }
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
            _userMessage.value = "تم حذف الفاتورة"
        }
    }

    fun toggleCompleted(id: Long) {
        viewModelScope.launch {
            val success = repository.toggleCompletedStatus(id)
            if (success) {
                _userMessage.value = "تم تحديث حالة الفاتورة"
            }
        }
    }

    // Company Settings
    private val _companySettings = MutableStateFlow(com.example.model.CompanySettings.load(getApplication()))
    val companySettings: StateFlow<com.example.model.CompanySettings> = _companySettings.asStateFlow()

    fun updateCompanySettings(settings: com.example.model.CompanySettings) {
        com.example.model.CompanySettings.save(getApplication(), settings)
        _companySettings.value = settings
        _userMessage.value = "تم حفظ بيانات المؤسسة بنجاح ✅"
    }

    // Product Catalog Management
    fun addProductToCatalog(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            _userMessage.value = "تمت إضافة المنتج إلى الدليل بنجاح"
        }
    }

    fun updateProductInCatalog(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            _userMessage.value = "تم تحديث بيانات السلعة بنجاح"
        }
    }

    fun deleteProductFromCatalog(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _userMessage.value = "تم حذف السلعة من الدليل"
        }
    }

    // Quick POS Sale Direct Save
    fun saveQuickSale(
        customerName: String,
        items: List<InvoiceItem>,
        paymentMethod: PaymentMethod,
        onSuccess: (Long) -> Unit
    ) {
        if (items.isEmpty()) {
            _userMessage.value = "يرجى إضافة سلعة واحدة على الأقل"
            return
        }
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyMMdd-HHmm", Locale.US)
        val invoiceNumber = "POS-${dateFormat.format(Date(now))}"
        val total = items.sumOf { it.total }

        val quickInvoice = Invoice(
            id = 0L,
            invoiceNumber = invoiceNumber,
            customerName = customerName.ifBlank { "عميل نقدي" },
            customerPhone = "",
            customerEmail = "",
            issueDate = now,
            dueDate = now,
            status = InvoiceStatus.COMPLETED,
            paymentMethod = paymentMethod,
            items = items,
            discountAmount = 0.0,
            isDiscountPercentage = false,
            taxPercentage = 0.0,
            paidAmount = total,
            notes = "إيصال بيع فوري - الوضع السريع",
            terms = "شكراً لتعاملكم معنا 🙏",
            createdAt = now,
            updatedAt = now,
            completedAt = now,
            isEditedAfterCompletion = false
        )

        viewModelScope.launch {
            val id = repository.saveInvoice(quickInvoice)
            _userMessage.value = "تم إصدار الإيصال وحفظ الفاتورة بنجاح ✅"
            onSuccess(id)
        }
    }
}
