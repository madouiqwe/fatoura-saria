package com.madoui.fatoura.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * تخزين محلي بسيط وموثوق (ملف JSON داخل مجلد التطبيق).
 * كل عملية تعديل تحفظ الملف فوراً، والواجهة تُحدَّث عبر StateFlow.
 */
class FatouraRepository(context: Context) {

    private val file = File(context.applicationContext.filesDir, "fatoura_data.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    // ---------------------------------------------------------------- قراءة/كتابة

    private fun load(): AppData {
        val loaded = runCatching {
            if (file.exists()) json.decodeFromString<AppData>(file.readText()) else null
        }.getOrNull()
        if (loaded != null) return loaded
        val fresh = AppData(
            products = demoProducts(),
            nextProductId = 17L,
            seeded = true
        )
        persist(fresh)
        return fresh
    }

    private fun persist(data: AppData) {
        runCatching {
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(json.encodeToString(AppData.serializer(), data))
            if (file.exists()) file.delete()
            temp.renameTo(file)
        }
    }

    @Synchronized
    private fun update(transform: (AppData) -> AppData): AppData {
        val next = transform(_data.value)
        _data.value = next
        persist(next)
        return next
    }

    // ---------------------------------------------------------------- المنتجات

    fun addProduct(product: Product) = update { data ->
        data.copy(
            products = data.products + product.copy(id = data.nextProductId),
            nextProductId = data.nextProductId + 1
        )
    }

    fun updateProduct(product: Product) = update { data ->
        data.copy(products = data.products.map { if (it.id == product.id) product else it })
    }

    fun deleteProduct(productId: Long) = update { data ->
        data.copy(products = data.products.filterNot { it.id == productId })
    }

    // ---------------------------------------------------------------- الفواتير

    /** يحفظ فاتورة جديدة أو يحدّث فاتورة موجودة، ويعيد معرّفها. */
    fun saveInvoice(draft: Invoice): Long {
        var savedId = draft.id
        update { data ->
            if (draft.id == 0L) {
                savedId = data.nextInvoiceId
                val number = draft.number.ifBlank { data.nextInvoiceNumber.toString() }
                val invoice = draft.copy(id = savedId, number = number)
                data.copy(
                    invoices = data.invoices + invoice,
                    nextInvoiceId = data.nextInvoiceId + 1,
                    nextInvoiceNumber = data.nextInvoiceNumber + 1
                )
            } else {
                data.copy(invoices = data.invoices.map { if (it.id == draft.id) draft else it })
            }
        }
        return savedId
    }

    fun deleteInvoice(invoiceId: Long) = update { data ->
        data.copy(invoices = data.invoices.filterNot { it.id == invoiceId })
    }

    fun setInvoiceStatus(invoiceId: Long, status: InvoiceStatus) = update { data ->
        data.copy(invoices = data.invoices.map { if (it.id == invoiceId) it.copy(status = status) else it })
    }

    fun updateSettings(settings: CompanySettings) = update { data ->
        data.copy(settings = settings)
    }

    // ---------------------------------------------------------------- بيانات تجريبية

    private fun demoProducts(): List<Product> = listOf(
        Product(1, "مرساة متوسطة", "تثبيت", "6130000000011", 420.00, 10),
        Product(2, "مرساة كان متوسطة", "تثبيت", "6130000000028", 550.00, 12),
        Product(3, "مرساة كان كبيرة", "تثبيت", "6130000000035", 600.00, 8),
        Product(4, "برغي خشب 4×40", "براغي", "6130000000042", 12.00, 100),
        Product(5, "برغي خشب 5×50", "براغي", "6130000000059", 18.00, 100),
        Product(6, "صامولة 8 مم", "صواميل", "6130000000066", 5.00, 200),
        Product(7, "وردة 8 مم", "وردات", "6130000000073", 3.00, 500),
        Product(8, "مسمار تثبيت 6 مم", "مسامير", "6130000000080", 8.00, 250),
        Product(9, "سلك كهربائي 1.5 مم", "كهرباء", "6130000000097", 85.00, 100),
        Product(10, "قابس ثلاثي", "كهرباء", "6130000000103", 120.00, 10),
        Product(11, "مفتاح إضاءة", "كهرباء", "6130000000110", 250.00, 12),
        Product(12, "شريط لاصق عازل", "كهرباء", "6130000000127", 60.00, 24),
        Product(13, "قفل باب أمان", "أقفال", "6130000000134", 950.00, 6),
        Product(14, "مقبض باب", "أكسسوارات", "6130000000141", 480.00, 10),
        Product(15, "طقم مفك براغي", "عدة", "6130000000158", 320.00, 12),
        Product(16, "زاوية حديد 4×4", "حديد", "6130000000165", 280.00, 15)
    )
}
