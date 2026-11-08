package com.example.data

import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.SaleUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao
) {

    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices().map { entities ->
        entities.map { it.toDomain() }
    }

    val allProducts: Flow<List<Product>> = productDao.getAllProducts().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)?.toDomain()
    }

    suspend fun saveInvoice(invoice: Invoice): Long {
        val now = System.currentTimeMillis()
        val isNew = invoice.id == 0L

        // Determine completion state and post-completion edits
        val isCompleted = invoice.status == InvoiceStatus.COMPLETED
        val completedTimestamp = when {
            isCompleted && invoice.completedAt == null -> now
            !isCompleted -> null
            else -> invoice.completedAt
        }

        // If it was already completed before, mark as edited after completion
        val editedAfterCompletion = if (!isNew && (invoice.completedAt != null || invoice.isEditedAfterCompletion)) {
            true
        } else {
            invoice.isEditedAfterCompletion
        }

        val toSave = invoice.copy(
            updatedAt = now,
            completedAt = completedTimestamp,
            isEditedAfterCompletion = editedAfterCompletion
        )

        return invoiceDao.insertInvoice(InvoiceEntity.fromDomain(toSave))
    }

    suspend fun deleteInvoice(id: Long) {
        invoiceDao.deleteInvoiceById(id)
    }

    suspend fun duplicateInvoice(originalId: Long): Long? {
        val original = getInvoiceById(originalId) ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0L,
            invoiceNumber = generateInvoiceNumber("INV-COPY"),
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            status = InvoiceStatus.PENDING,
            paidAmount = 0.0,
            isEditedAfterCompletion = false
        )
        return invoiceDao.insertInvoice(InvoiceEntity.fromDomain(copy))
    }

    suspend fun toggleCompletedStatus(invoiceId: Long): Boolean {
        val current = getInvoiceById(invoiceId) ?: return false
        val now = System.currentTimeMillis()
        val newStatus = if (current.status == InvoiceStatus.COMPLETED) {
            InvoiceStatus.PENDING
        } else {
            InvoiceStatus.COMPLETED
        }
        val newPaidAmount = if (newStatus == InvoiceStatus.COMPLETED) {
            current.grandTotal
        } else {
            current.paidAmount
        }

        val updated = current.copy(
            status = newStatus,
            paidAmount = newPaidAmount,
            updatedAt = now,
            completedAt = if (newStatus == InvoiceStatus.COMPLETED) now else null
        )
        invoiceDao.updateInvoice(InvoiceEntity.fromDomain(updated))
        return true
    }

    suspend fun saveProduct(product: Product) {
        productDao.insertProduct(ProductEntity.fromDomain(product))
    }

    suspend fun deleteProduct(productId: String) {
        productDao.deleteProductById(productId)
    }

    suspend fun populateSampleDataIfEmpty() {
        populateSampleProductsIfEmpty()

        val count = invoiceDao.getInvoiceCount()
        if (count > 0) return

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // Invoice 1: Exactly matching Screenshot 2 (رقم الفاتورة: 868052, العميل: نصرو!, الإجمالي: 17,850.00 د.ج)
        val invoice868052 = Invoice(
            id = 0L,
            invoiceNumber = "868052",
            customerName = "نصرو!",
            customerPhone = "0550 12 34 56",
            customerEmail = "nasro@example.com",
            issueDate = 1791331200000L, // 2026/10/07
            dueDate = 1791936000000L,
            status = InvoiceStatus.COMPLETED,
            paymentMethod = PaymentMethod.CASH,
            items = listOf(
                InvoiceItem(
                    id = "item-1-868052",
                    name = "مرساي متوسط",
                    packageInfo = "كرتونة (10 حبات)",
                    unitsPerPackage = 10.0,
                    packagesCount = 1.0,
                    singlePiecesCount = 10.0,
                    saleUnit = SaleUnit.PACKAGE,
                    quantity = 10.0,
                    unitPrice = 420.00,
                    description = "مرساي متوسط فاخر"
                ),
                InvoiceItem(
                    id = "item-2-868052",
                    name = "مرساي كتان متوسط",
                    packageInfo = "كرتونة (10 حبات)",
                    unitsPerPackage = 10.0,
                    packagesCount = 1.5,
                    singlePiecesCount = 15.0,
                    saleUnit = SaleUnit.PIECE,
                    quantity = 15.0,
                    unitPrice = 550.00,
                    description = "مرساي كتان متوسط ممتاز"
                ),
                InvoiceItem(
                    id = "item-3-868052",
                    name = "مرساي كتان كبير",
                    packageInfo = "كرتونة (10 حبات)",
                    unitsPerPackage = 10.0,
                    packagesCount = 0.9,
                    singlePiecesCount = 9.0,
                    saleUnit = SaleUnit.PIECE,
                    quantity = 9.0,
                    unitPrice = 600.00,
                    description = "مرساي كتان كبير جودة عالية"
                )
            ),
            discountAmount = 0.0,
            isDiscountPercentage = false,
            taxPercentage = 0.0,
            paidAmount = 17850.00, // 4,200 + 8,250 + 5,400 = 17,850.00 د.ج
            notes = "تم الدفع نقداً عند الاستلام.",
            terms = "شكراً لتعاملكم معنا 🙏",
            createdAt = 1791331200000L,
            updatedAt = 1791331200000L,
            completedAt = 1791331200000L,
            isEditedAfterCompletion = false
        )

        // Invoice 2: Second invoice to make total invoices count = 2
        val invoice868053 = Invoice(
            id = 0L,
            invoiceNumber = "868053",
            customerName = "مؤسسة الأناقة للتجارة",
            customerPhone = "0551 77 88 99",
            customerEmail = "elegance@example.dz",
            issueDate = now - (1 * dayMillis),
            dueDate = now + (7 * dayMillis),
            status = InvoiceStatus.PENDING,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            items = listOf(
                InvoiceItem(
                    id = "item-1-868053",
                    name = "قماش قطن فاخر",
                    packageInfo = "طاقة (12 متر)",
                    unitsPerPackage = 12.0,
                    packagesCount = 4.0,
                    singlePiecesCount = 0.0,
                    saleUnit = SaleUnit.PACKAGE,
                    quantity = 48.0,
                    unitPrice = 350.00,
                    description = "طاقة قطن تركي أصلي"
                )
            ),
            discountAmount = 0.0,
            isDiscountPercentage = false,
            taxPercentage = 0.0,
            paidAmount = 0.0,
            notes = "قيد السداد عبر تحويل بنكي.",
            terms = "شكراً لتعاملكم معنا 🙏",
            createdAt = now - (1 * dayMillis),
            updatedAt = now - (1 * dayMillis),
            completedAt = null,
            isEditedAfterCompletion = false
        )

        invoiceDao.insertInvoice(InvoiceEntity.fromDomain(invoice868052))
        invoiceDao.insertInvoice(InvoiceEntity.fromDomain(invoice868053))
    }

    private suspend fun populateSampleProductsIfEmpty() {
        if (productDao.getProductCount() > 0) return

        // 16 registered items matching Screenshot 1 ("16 سلعة مسجلة")
        val sampleCatalog = listOf(
            Product(
                id = "p-mrs-med",
                name = "مرساي متوسط",
                defaultUnitPrice = 420.00,
                unitsPerPackage = 10.0,
                packageTypeName = "كرتونة",
                description = "قماش مرساي متوسط الجودة الممتازة"
            ),
            Product(
                id = "p-mrs-kt-med",
                name = "مرساي كتان متوسط",
                defaultUnitPrice = 550.00,
                unitsPerPackage = 10.0,
                packageTypeName = "كرتونة",
                description = "كتان مرساي متوسط خفيف ومريح"
            ),
            Product(
                id = "p-mrs-kt-lg",
                name = "مرساي كتان كبير",
                defaultUnitPrice = 600.00,
                unitsPerPackage = 10.0,
                packageTypeName = "كرتونة",
                description = "كتان مرساي مقاس كبير جودة راقية"
            ),
            Product(
                id = "p-cotton-lux",
                name = "قماش قطن فاخر",
                defaultUnitPrice = 350.00,
                unitsPerPackage = 12.0,
                packageTypeName = "طاقة",
                description = "قطن مصري أصلي 100%"
            ),
            Product(
                id = "p-summer-flower",
                name = "قماش صيفي مشجر",
                defaultUnitPrice = 480.00,
                unitsPerPackage = 15.0,
                packageTypeName = "طاقة",
                description = "قماش صيفي خفيف للمناسبات"
            ),
            Product(
                id = "p-winter-kash",
                name = "قماش شتوي كشمير",
                defaultUnitPrice = 850.00,
                unitsPerPackage = 8.0,
                packageTypeName = "صندوق",
                description = "كشمير شتوي دافئ وعالي الجودة"
            ),
            Product(
                id = "p-silk-pure",
                name = "حرير طبيعي ممتاز",
                defaultUnitPrice = 1200.00,
                unitsPerPackage = 6.0,
                packageTypeName = "صندوق",
                description = "حرير طبيعي ناعم ومطرز"
            ),
            Product(
                id = "p-mrs-black",
                name = "مرساي سادة أسود",
                defaultUnitPrice = 400.00,
                unitsPerPackage = 10.0,
                packageTypeName = "كرتونة",
                description = "مرساي أسود كلاسيكي فاخر"
            ),
            Product(
                id = "p-jeans-cotton",
                name = "قماش جينز قطني",
                defaultUnitPrice = 650.00,
                unitsPerPackage = 12.0,
                packageTypeName = "طاقة",
                description = "جينز ثقيل ومتين للملابس"
            ),
            Product(
                id = "p-crepe-korean",
                name = "كريب كوري فاخر",
                defaultUnitPrice = 500.00,
                unitsPerPackage = 10.0,
                packageTypeName = "ثوب",
                description = "كريب كوري أصلي انسيابي"
            ),
            Product(
                id = "p-chiffon-color",
                name = "شيفون مطرز ملون",
                defaultUnitPrice = 380.00,
                unitsPerPackage = 15.0,
                packageTypeName = "باقة",
                description = "شيفون ناعم وألوان زاهية"
            ),
            Product(
                id = "p-linen-wash",
                name = "لينن طبيعي مغسول",
                defaultUnitPrice = 720.00,
                unitsPerPackage = 10.0,
                packageTypeName = "طاقة",
                description = "لينن أوروبي فاخر معالج"
            ),
            Product(
                id = "p-wool-eng",
                name = "صوف إنجليزي رجالي",
                defaultUnitPrice = 1500.00,
                unitsPerPackage = 5.0,
                packageTypeName = "صندوق",
                description = "صوف بريطاني خالص للبذلات"
            ),
            Product(
                id = "p-taffeta-lin",
                name = "بطانة تفتا ناعمة",
                defaultUnitPrice = 180.00,
                unitsPerPackage = 24.0,
                packageTypeName = "كرتونة",
                description = "تفتا لمعة هادئة لتبطين الأثواب"
            ),
            Product(
                id = "p-satin-italy",
                name = "ساتان إيطالي لامع",
                defaultUnitPrice = 580.00,
                unitsPerPackage = 10.0,
                packageTypeName = "ثوب",
                description = "ساتان حريري للفساتين والسهرات"
            ),
            Product(
                id = "p-thread-gold",
                name = "خيط تطريز مقصب",
                defaultUnitPrice = 95.00,
                unitsPerPackage = 20.0,
                packageTypeName = "شد",
                description = "شد خيوط ذهبية وفضية عالية الجودة"
            )
        )

        productDao.insertAll(sampleCatalog.map { ProductEntity.fromDomain(it) })
    }

    fun generateInvoiceNumber(prefix: String = "INV"): String {
        val dateFormat = SimpleDateFormat("yyMMdd-HHmm", Locale.US)
        return "$prefix-${dateFormat.format(Date())}"
    }
}
