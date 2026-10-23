package com.madoui.fatoura.data

import kotlinx.serialization.Serializable

/** وحدة البيع داخل الفاتورة: حبة مفردة أو حزمة كاملة. */
@Serializable
enum class SaleUnit {
    PIECE,
    PACKAGE;

    val label: String
        get() = if (this == PACKAGE) "حزمة" else "حبة"
}

@Serializable
enum class PaymentMethod {
    CASH,
    CREDIT,
    CARD,
    TRANSFER;

    val label: String
        get() = when (this) {
            CASH -> "نقداً"
            CREDIT -> "آجل"
            CARD -> "بطاقة"
            TRANSFER -> "تحويل"
        }
}

@Serializable
enum class InvoiceStatus {
    PAID,
    UNPAID,
    PARTIAL;

    val label: String
        get() = when (this) {
            PAID -> "مدفوعة"
            UNPAID -> "غير مدفوعة"
            PARTIAL -> "مدفوعة جزئياً"
        }
}

/**
 * المنتج مع سعة الحزمة الخاصة به.
 * [packageCapacity] هو عدد القطع المفردة داخل حزمة هذا المنتج (10، 12، 24 ...).
 */
@Serializable
data class Product(
    val id: Long = 0L,
    val name: String = "",
    val category: String = "",
    val barcode: String = "",
    val pricePerPiece: Double = 0.0,
    val packageCapacity: Int = 1
) {
    val packagePrice: Double
        get() = pricePerPiece * packageCapacity.coerceAtLeast(1)

    /** سعر الوحدة المختارة: الحبة = سعر الحبة، الحزمة = سعر الحبة × سعة الحزمة. */
    fun priceFor(unit: SaleUnit): Double =
        if (unit == SaleUnit.PACKAGE) packagePrice else pricePerPiece
}

/** بند محفوظ داخل الفاتورة، مع لقطة (snapshot) لسعة الحزمة وقت البيع. */
@Serializable
data class InvoiceItem(
    val id: Long = 0L,
    val productId: Long? = null,
    val name: String = "",
    val unit: SaleUnit = SaleUnit.PIECE,
    val quantity: Double = 1.0,
    val unitsPerPackage: Int = 1,
    val unitPrice: Double = 0.0
) {
    val totalPieces: Double
        get() = quantity * if (unit == SaleUnit.PACKAGE) unitsPerPackage.coerceAtLeast(1) else 1

    val lineTotal: Double
        get() = quantity * unitPrice

    val quantityLabel: String
        get() = "${com.madoui.fatoura.util.Fmt.qty(quantity)} ${unit.label}"
}

@Serializable
data class Invoice(
    val id: Long = 0L,
    val number: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val customerName: String = "",
    val customerPhone: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val status: InvoiceStatus = InvoiceStatus.PAID,
    val items: List<InvoiceItem> = emptyList(),
    val discount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtotal: Double
        get() = items.sumOf { it.lineTotal }

    val total: Double
        get() = (subtotal - discount).coerceAtLeast(0.0)

    val totalPieces: Double
        get() = items.sumOf { it.totalPieces }
}

@Serializable
data class CompanySettings(
    val name: String = "اسم شركتك",
    val phone: String = "0555 55 55 55",
    val address: String = "العنوان",
    val footerNote: String = "شكراً لتعاملكم معنا"
)

/** كل بيانات التطبيق، تُحفظ محلياً في ملف JSON. */
@Serializable
data class AppData(
    val products: List<Product> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val settings: CompanySettings = CompanySettings(),
    val nextProductId: Long = 1L,
    val nextInvoiceId: Long = 1L,
    val nextInvoiceNumber: Long = 868052L,
    val seeded: Boolean = false
) {
    val collectedSales: Double
        get() = invoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.total }
}
