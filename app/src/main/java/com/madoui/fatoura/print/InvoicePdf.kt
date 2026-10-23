package com.madoui.fatoura.print

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.madoui.fatoura.data.CompanySettings
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceItem
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.data.SaleUnit
import com.madoui.fatoura.util.Fmt
import java.io.File
import java.io.FileOutputStream

/**
 * توليد فاتورة PDF احترافية بمقاس A4 مع دعم كامل للعربية (RTL).
 * تعتمد على محرك النصوص في أندرويد لتشكيل الحروف العربية تلقائياً.
 */
object InvoicePdf {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    private const val COLOR_TEAL = 0xFF00796B.toInt()
    private const val COLOR_TEAL_DARK = 0xFF005B52.toInt()
    private const val COLOR_TEAL_SOFT = 0xFFE6F4F1.toInt()
    private const val COLOR_TEXT = 0xFF1F2933.toInt()
    private const val COLOR_MUTED = 0xFF6B7280.toInt()
    private const val COLOR_LINE = 0xFFE6E8EC.toInt()
    private const val COLOR_GREEN = 0xFF2E7D32.toInt()
    private const val COLOR_GREEN_SOFT = 0xFFE6F2E7.toInt()
    private const val COLOR_RED = 0xFFC62828.toInt()
    private const val COLOR_RED_SOFT = 0xFFFDE8E8.toInt()
    private const val COLOR_AMBER = 0xFF9A6B00.toInt()
    private const val COLOR_AMBER_SOFT = 0xFFFFF6DC.toInt()
    private const val COLOR_ROW_ALT = 0xFFF8FAFA.toInt()

    fun create(context: Context, invoice: Invoice, company: CompanySettings): File {
        val document = PdfDocument()
        val writer = PageWriter(document)

        drawHeader(writer, invoice, company)

        var y = drawMeta(writer, invoice, 150f)
        y = drawTableHeader(writer, y)

        invoice.items.forEachIndexed { index, item ->
            val rowHeight = if (item.unit == SaleUnit.PACKAGE) 34f else 26f
            if (y + rowHeight > PAGE_HEIGHT - 120f) {
                writer.finishPage()
                writer.startPage()
                y = drawTableHeader(writer, 60f)
            }
            y = drawItemRow(writer, item, index, y, rowHeight)
        }

        y += 14f
        y = drawTotals(writer, invoice, y)

        if (y + 60f < PAGE_HEIGHT - 40f) {
            drawFooter(writer, company)
        }

        writer.finish()

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${invoice.number}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    // ------------------------------------------------------------------ الترويسة

    private fun drawHeader(writer: PageWriter, invoice: Invoice, company: CompanySettings) {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN

        val namePaint = textPaint(15f, COLOR_TEAL_DARK, bold = true)
        canvas.drawText(company.name.ifBlank { "اسم شركتك" }, right, 52f, namePaint)

        val small = textPaint(9f, COLOR_MUTED)
        canvas.drawText("هاتف : ${company.phone}", right, 70f, small)
        canvas.drawText("العنوان : ${company.address}", right, 84f, small)

        val titlePaint = textPaint(19f, COLOR_TEAL, bold = true)
        canvas.drawText("فاتورة مبيعات", MARGIN, 52f, titlePaint)

        val meta = textPaint(9f, COLOR_MUTED)
        canvas.drawText("رقم الفاتورة : ${invoice.number}", MARGIN, 72f, meta)
        canvas.drawText("التاريخ : ${Fmt.date(invoice.dateMillis)}", MARGIN, 86f, meta)

        val line = Paint().apply {
            color = COLOR_LINE
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(MARGIN, 100f, right, 100f, line)
    }

    // ------------------------------------------------------------------ بيانات الفاتورة

    private fun drawMeta(writer: PageWriter, invoice: Invoice, startY: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val boxTop = startY
        val boxBottom = startY + 58f

        val boxPaint = Paint().apply {
            color = 0xFFF5F7F7.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(left, boxTop, right, boxBottom), 8f, 8f, boxPaint)

        // شارة الحالة في أقصى اليمين
        val statusText = invoice.status.label
        val statusPaint = textPaint(8.5f, Color.WHITE, bold = true)
        val badgeWidth = statusPaint.measureText(statusText) + 22f
        val badgeRight = right - 10f
        val badgeLeft = badgeRight - badgeWidth
        val badgeTop = boxTop + 10f
        val badgeBottom = badgeTop + 20f
        val badgeColor = when (invoice.status) {
            InvoiceStatus.PAID -> COLOR_GREEN
            InvoiceStatus.UNPAID -> COLOR_RED
            InvoiceStatus.PARTIAL -> COLOR_AMBER
        }
        val badgePaint = Paint().apply { color = badgeColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(badgeLeft, badgeTop, badgeRight, badgeBottom), 10f, 10f, badgePaint)

        statusPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(statusText, (badgeLeft + badgeRight) / 2f, badgeBottom - 6f, statusPaint)

        val labelPaint = textPaint(8f, COLOR_MUTED)
        val valuePaint = textPaint(9.5f, COLOR_TEXT, bold = true)

        canvas.drawText("العميل", right, boxTop + 40f, labelPaint.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText(
            invoice.customerName.ifBlank { "عميل نقدي" },
            right - 38f,
            boxTop + 40f,
            valuePaint.apply { textAlign = Paint.Align.RIGHT }
        )

        canvas.drawText(
            "طريقة الدفع : ${invoice.paymentMethod.label}",
            right,
            boxTop + 52f,
            textPaint(8f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        )

        // معلومات إضافية في الجهة اليسرى
        canvas.drawText(
            "عدد الأصناف : ${invoice.items.size}",
            left + 10f,
            boxTop + 26f,
            textPaint(8f, COLOR_MUTED).apply { textAlign = Paint.Align.LEFT }
        )
        canvas.drawText(
            "إجمالي القطع : ${Fmt.qty(invoice.totalPieces)}",
            left + 10f,
            boxTop + 42f,
            textPaint(8f, COLOR_MUTED).apply { textAlign = Paint.Align.LEFT }
        )

        return boxBottom + 18f
    }

    // ------------------------------------------------------------------ الجدول

    private val colName = 220f
    private val colQty = 92f
    private val colPrice = 90f
    private val colTotal = 113f

    private fun drawTableHeader(writer: PageWriter, top: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val height = 26f

        val bg = Paint().apply { color = COLOR_TEAL_DARK }
        canvas.drawRoundRect(RectF(left, top, right, top + height), 4f, 4f, bg)

        val paint = textPaint(9f, Color.WHITE, bold = true)
        paint.textAlign = Paint.Align.CENTER
        val baseline = top + 17f

        val nameCenter = right - colName / 2f
        val qtyCenter = right - colName - colQty / 2f
        val priceCenter = right - colName - colQty - colPrice / 2f
        val totalCenter = left + colTotal / 2f

        canvas.drawText("الصنف", nameCenter, baseline, paint)
        canvas.drawText("الكمية", qtyCenter, baseline, paint)
        canvas.drawText("السعر", priceCenter, baseline, paint)
        canvas.drawText("الإجمالي", totalCenter, baseline, paint)

        return top + height
    }

    private fun drawItemRow(
        writer: PageWriter,
        item: InvoiceItem,
        index: Int,
        top: Float,
        height: Float
    ): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val bottom = top + height

        if (index % 2 == 1) {
            val alt = Paint().apply { color = COLOR_ROW_ALT }
            canvas.drawRect(RectF(left, top, right, bottom), alt)
        }

        val namePaint = textPaint(9f, COLOR_TEXT, bold = true)
        val subPaint = textPaint(7f, COLOR_MUTED)
        val cellPaint = textPaint(8.5f, COLOR_TEXT)
        val totalPaint = textPaint(9f, COLOR_TEXT, bold = true)

        // الصنف (يمين)
        val nameRight = right - 6f
        val maxNameWidth = colName - 14f
        val nameBaseline = if (item.unit == SaleUnit.PACKAGE) top + 13f else top + height / 2f + 3f
        val nameText = ellipsize(item.name, namePaint, maxNameWidth)
        namePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(nameText, nameRight, nameBaseline, namePaint)

        if (item.unit == SaleUnit.PACKAGE) {
            subPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "حزمة ${item.unitsPerPackage} حبة  •  ${Fmt.qty(item.totalPieces)} حبة إجمالاً",
                nameRight,
                top + 24f,
                subPaint
            )
        }

        // الكمية
        val qtyCenter = right - colName - colQty / 2f
        cellPaint.textAlign = Paint.Align.CENTER
        val qtyText = "${Fmt.qty(item.quantity)} ${item.unit.label}"
        canvas.drawText(qtyText, qtyCenter, top + height / 2f + 3f, cellPaint)

        // السعر
        val priceCenter = right - colName - colQty - colPrice / 2f
        canvas.drawText(Fmt.amount(item.unitPrice), priceCenter, top + height / 2f + 3f, cellPaint)

        // الإجمالي
        val totalCenter = left + colTotal / 2f
        totalPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(Fmt.amount(item.lineTotal), totalCenter, top + height / 2f + 3f, totalPaint)

        val line = Paint().apply { color = COLOR_LINE; strokeWidth = 0.8f }
        canvas.drawLine(left, bottom, right, bottom, line)

        return bottom
    }

    // ------------------------------------------------------------------ المجاميع

    private fun drawTotals(writer: PageWriter, invoice: Invoice, startY: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN

        var y = startY + 10f

        val labelPaint = textPaint(9f, COLOR_MUTED)
        val valuePaint = textPaint(9.5f, COLOR_TEXT, bold = true)

        labelPaint.textAlign = Paint.Align.RIGHT
        valuePaint.textAlign = Paint.Align.RIGHT

        canvas.drawText("المجموع الفرعي", right - 90f, y, labelPaint)
        canvas.drawText(Fmt.money(invoice.subtotal), right, y, valuePaint)
        y += 16f

        if (invoice.discount > 0.0) {
            canvas.drawText("الخصم", right - 90f, y, labelPaint)
            canvas.drawText("- ${Fmt.money(invoice.discount)}", right, y, valuePaint)
            y += 16f
        }

        y += 4f
        val boxTop = y
        val boxBottom = y + 26f
        val boxPaint = Paint().apply { color = COLOR_TEAL_SOFT }
        canvas.drawRoundRect(RectF(right - 250f, boxTop, right, boxBottom), 6f, 6f, boxPaint)

        val finalLabel = textPaint(10f, COLOR_TEAL_DARK, bold = true)
        finalLabel.textAlign = Paint.Align.RIGHT
        canvas.drawText("الإجمالي النهائي", right - 10f, boxTop + 17f, finalLabel)

        val finalValue = textPaint(12f, COLOR_TEAL, bold = true)
        finalValue.textAlign = Paint.Align.RIGHT
        canvas.drawText(Fmt.money(invoice.total), right - 140f, boxTop + 18f, finalValue)

        return boxBottom
    }

    private fun drawFooter(writer: PageWriter, company: CompanySettings) {
        val canvas = writer.canvas
        val paint = textPaint(9f, COLOR_MUTED)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            company.footerNote.ifBlank { "شكراً لتعاملكم معنا" },
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 44f,
            paint
        )

        val small = textPaint(7.5f, COLOR_LINE)
        small.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "تم إنشاؤها بواسطة فاتورة سريعة v2.0",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 30f,
            small
        )
    }

    // ------------------------------------------------------------------ أدوات

    private fun textPaint(size: Float, color: Int, bold: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        return TextUtils.ellipsize(text, TextPaint(paint), maxWidth, TextUtils.TruncateAt.END).toString()
    }

    /** إدارة صفحات PDF: إنهاء وبدء صفحات جديدة. */
    private class PageWriter(private val document: PdfDocument) {
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        private var pageNumber = 0

        init {
            startPage()
        }

        fun startPage() {
            pageNumber += 1
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            canvas = page.canvas
            canvas.drawColor(Color.WHITE)
        }

        fun finishPage() {
            document.finishPage(page)
        }

        fun finish() {
            finishPage()
        }
    }
}
