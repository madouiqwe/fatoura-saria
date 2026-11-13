package com.madoui.fatoura.data

import kotlinx.serialization.Serializable
import com.madoui.fatoura.util.Fmt

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

/** نوع الفاتورة: مبيعات أصناف عادية، أو إيصال من الحاسبة السريعة. */
@Serializable
enum class InvoiceKind {
    SALE,
    CALCULATOR
}

/** التسمية المعتمدة لأي سطر مبلغ مباشر أُضيف من الحاسبة دون عملية حسابية. */
const val DIRECT_AMOUNT_LABEL = "مبلغ مباشر"

/** تسمية قديمة كانت تُحفظ في الإصدارات السابقة؛ تُدعم للقراءة فقط. */
private const val LEGACY_DIRECT_AMOUNT_LABEL = "مبلغ إجمالي سريع"

/** يطابق تعبيراً حسابياً بسيطاً مثل «350 × 12» أو «5+3». */
private val CALC_EXPRESSION = Regex("""^\s*[\d.,]+\s*[+\-×÷*/]\s*[\d.,]+\s*$""")

/** يطابق أي نص ناتج عن الحاسبة: أرقام وعمليات حسابية فقط بلا أسماء. */
private val CALC_ANY_EXPRESSION = Regex("""^\s*[\d.,\s+\-−×÷x*/]+\s*$""")

/**
 * يعيد اسم السطر بعد تعديل الكمية أو السعر:
 * سطر «250 × 10» يصبح «250 × 12» عند تغيير الكمية، والمبلغ المباشر
 * يحتفظ بتسميته، وأسماء المنتجات المكتوبة يدوياً لا تُمس.
 */
fun refreshedCalcItemName(originalName: String, unitPrice: Double, quantity: Double): String {
    val name = originalName.trim()
    if (name.isEmpty()) return DIRECT_AMOUNT_LABEL
    if (name == DIRECT_AMOUNT_LABEL || name == LEGACY_DIRECT_AMOUNT_LABEL) return name
    if (!CALC_ANY_EXPRESSION.matches(name) || !name.any { it.isDigit() }) return originalName
    val price = com.madoui.fatoura.util.Fmt.amount(unitPrice)
    val qty = com.madoui.fatoura.util.Fmt.qty(quantity)
    return if (quantity > 1.0) "$price × $qty" else price
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
    /** إجمالي عدد الحبات الفعلي لهذا البند */
    val totalPieces: Double
        get() = quantity * if (unit == SaleUnit.PACKAGE) unitsPerPackage.coerceAtLeast(1) else 1

    /** الإجمالي المالي للبند */
    val lineTotal: Double
        get() = quantity * unitPrice

    /** سعر الحبة الواحدة المفردة دائمًا */
    val piecePrice: Double
        get() = if (unit == SaleUnit.PACKAGE && unitsPerPackage > 1) {
            unitPrice / unitsPerPackage.toDouble()
        } else {
            unitPrice
        }

    /** نص توضيحي للكمية يظهر عدد الحبات بدقة واحترافية */
    val piecesDisplay: String
        get() = if (unit == SaleUnit.PACKAGE) {
            "${com.madoui.fatoura.util.Fmt.qty(totalPieces)} حبة (${com.madoui.fatoura.util.Fmt.qty(quantity)} حزمة × $unitsPerPackage)"
        } else {
            "${com.madoui.fatoura.util.Fmt.qty(quantity)} حبة"
        }

    val quantityLabel: String
        get() = piecesDisplay
}

/**
 * نص السطر في فواتير الحاسبة: العملية كاملة بنتيجتها «250 × 10 = 2500»،
 * والمبالغ المباشرة تُعرض بمبلغها، والأسطر الرقمية تُعرض كمبلغ فقط.
 */
fun calcLineText(item: InvoiceItem): String {
    val label = when (item.name.trim()) {
        "", DIRECT_AMOUNT_LABEL, LEGACY_DIRECT_AMOUNT_LABEL -> DIRECT_AMOUNT_LABEL
        else -> item.name.trim()
    }
    if (item.quantity > 1.0) {
        return "${Fmt.amount(item.unitPrice)} × ${Fmt.qty(item.quantity)} = ${Fmt.amount(item.lineTotal)}"
    }
    val digitsOnly = label.isNotEmpty() && label.all { it.isDigit() || it == '.' }
    return when {
        digitsOnly -> Fmt.money(item.lineTotal)
        label == DIRECT_AMOUNT_LABEL -> "${DIRECT_AMOUNT_LABEL} = ${Fmt.amount(item.lineTotal)}"
        else -> "$label = ${Fmt.amount(item.lineTotal)}"
    }
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
    val kind: InvoiceKind = InvoiceKind.SALE,
    val createdAt: Long = System.currentTimeMillis()
) {
    val subtotal: Double
        get() = items.sumOf { it.lineTotal }

    val total: Double
        get() = (subtotal - discount).coerceAtLeast(0.0)

    val totalPieces: Double
        get() = items.sumOf { it.totalPieces }

    /**
     * هل هذه فاتورة حاسبة؟ النوع الصريح هو المصدر الأساسي، ومع ذلك تُستنتج
     * الفواتير القديمة المحفوظة قبل إضافة حقل [kind] من شكل بنودها.
     */
    val isCalculator: Boolean
        get() = kind == InvoiceKind.CALCULATOR || matchesLegacyCalculatorShape()

    private fun matchesLegacyCalculatorShape(): Boolean {
        if (items.isEmpty()) return false
        if (items.any { it.productId != null }) return false
        return items.all { item ->
            val name = item.name.trim()
            name.isEmpty() ||
                name == DIRECT_AMOUNT_LABEL ||
                name == LEGACY_DIRECT_AMOUNT_LABEL ||
                CALC_EXPRESSION.matches(name)
        }
    }
}

@Serializable
data class CompanySettings(
    val name: String = "أولاد الطاهر للتجارة",
    val phone: String = "0668153994",
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
    val nextInvoiceNumber: Long = 868056L,
    val seeded: Boolean = false
) {
    val collectedSales: Double
        get() = invoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.total }
}
