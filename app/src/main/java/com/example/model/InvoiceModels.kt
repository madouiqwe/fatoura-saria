package com.example.model

import java.util.UUID

enum class InvoiceStatus(val labelAr: String, val labelEn: String) {
    COMPLETED("مكتملة / مدفوعة", "Completed / Paid"),
    PENDING("قيد الانتظار", "Pending"),
    PARTIALLY_PAID("مدفوعة جزئياً", "Partially Paid"),
    CANCELLED("ملغاة", "Cancelled")
}

enum class PaymentMethod(val labelAr: String, val labelEn: String) {
    CASH("نقداً", "Cash"),
    BANK_TRANSFER("تحويل بنكي", "Bank Transfer"),
    CARD("بطاقة مدى / فيزا", "Card"),
    CHEQUE("شيك", "Cheque")
}

/**
 * وحدة البيع: حزمة (كرتون/صندوق/ربطة) أو حبة مفردة (قطعة)
 */
enum class SaleUnit(val labelAr: String, val labelEn: String, val iconText: String) {
    PACKAGE("حزمة", "Package", "📦"),
    PIECE("حبة مفردة", "Piece", "🏷️")
}

/**
 * بيانات المنتج في دليل المنتجات (Catalog Product)
 * يحدد سعة الحزمة (عدد القطع الفردية في الحزمة) وسعر القطعة الافتراضي
 */
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val defaultUnitPrice: Double = 0.0,
    val unitsPerPackage: Double = 12.0, // سعة الحزمة لهذا المنتج بالتحديد (مثلاً 24، 10، إلخ)
    val packageTypeName: String = "كرتونة", // كرتونة، صندوق، باقة، شد، باكيت...
    val description: String = ""
)

/**
 * بند في الفاتورة يدعم:
 * 1. سعة الحزمة المخصصة لهذا المنتج (unitsPerPackage مثل 24 أو 10 أو 6)
 * 2. وحدة البيع المختارة (حزمة أو حبة مفردة)
 * 3. الحساب التلقائي للقطع الإجمالية والإجمالي المالي
 */
data class InvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val packageInfo: String = "",
    val unitsPerPackage: Double = 12.0,  // سعة الحزمة المحددة لهذا الصنف (مثلاً 24، 10، 6)
    val packagesCount: Double = 1.0,    // عدد الحزم إذا كان البيع بالحزمة
    val singlePiecesCount: Double = 0.0,// عدد الحبات إذا كان البيع بالحبة المفردة
    val saleUnit: SaleUnit = SaleUnit.PACKAGE, // نوع البيع: حزمة أو حبة مفردة
    val quantity: Double = if (saleUnit == SaleUnit.PACKAGE) (unitsPerPackage * packagesCount) else singlePiecesCount, // إجمالي عدد القطع الفعلي
    val unitPrice: Double = 0.0,        // سعر القطعة الواحدة
    val description: String = ""
) {
    val total: Double
        get() = if (saleUnit == SaleUnit.PACKAGE) {
            (packagesCount * unitsPerPackage * unitPrice).coerceAtLeast(0.0)
        } else {
            (quantity * unitPrice).coerceAtLeast(0.0)
        }

    val packagePrice: Double
        get() = (unitsPerPackage * unitPrice).coerceAtLeast(0.0)

    val effectiveQuantity: Double
        get() = if (saleUnit == SaleUnit.PACKAGE) (unitsPerPackage * packagesCount) else quantity

    /**
     * نص توضيحي للكمية والوحدة
     */
    val quantitySummaryAr: String
        get() = if (saleUnit == SaleUnit.PACKAGE) {
            val packsStr = if (packagesCount == packagesCount.toLong().toDouble()) packagesCount.toLong().toString() else packagesCount.toString()
            val totalPiecesStr = if (effectiveQuantity == effectiveQuantity.toLong().toDouble()) effectiveQuantity.toLong().toString() else effectiveQuantity.toString()
            "$packsStr حزمة ($totalPiecesStr قطعة)"
        } else {
            val piecesStr = if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else quantity.toString()
            "$piecesStr حبة مفردة"
        }
}

data class Invoice(
    val id: Long = 0L,
    val invoiceNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000), // Default 7 days
    val status: InvoiceStatus = InvoiceStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val items: List<InvoiceItem> = emptyList(),
    val discountAmount: Double = 0.0,
    val isDiscountPercentage: Boolean = false,
    val taxPercentage: Double = 15.0, // Default 15% VAT standard
    val paidAmount: Double = 0.0,
    val notes: String = "",
    val terms: String = "شكراً لتعاملكم معنا.",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isEditedAfterCompletion: Boolean = false
) {
    val subtotal: Double
        get() = items.sumOf { it.total }

    val calculatedDiscount: Double
        get() = if (isDiscountPercentage) {
            (subtotal * (discountAmount / 100.0)).coerceAtLeast(0.0)
        } else {
            discountAmount.coerceAtLeast(0.0)
        }

    val amountAfterDiscount: Double
        get() = (subtotal - calculatedDiscount).coerceAtLeast(0.0)

    val calculatedTax: Double
        get() = if (taxPercentage > 0) {
            amountAfterDiscount * (taxPercentage / 100.0)
        } else 0.0

    val grandTotal: Double
        get() = (amountAfterDiscount + calculatedTax).coerceAtLeast(0.0)

    val remainingBalance: Double
        get() = (grandTotal - paidAmount).coerceAtLeast(0.0)
}
