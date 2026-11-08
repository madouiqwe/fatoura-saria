package com.madoui.fatoura.print

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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

/** إنشاء فاتورة PDF احترافية بمقاس A4 مع دعم العربية. */
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
    private const val COLOR_RED = 0xFFC62828.toInt()
    private const val COLOR_AMBER = 0xFF9A6B00.toInt()
    private const val COLOR_ROW_ALT = 0xFFF7FAFA.toInt()

    fun create(context: Context, invoice: Invoice, company: CompanySettings): File {
        val document = PdfDocument()
        val writer = PageWriter(document)
        val calculatorInvoice = invoice.isCalculatorQuickInvoice()
        drawHeader(writer, invoice, company, calculatorInvoice)

        var y = drawMeta(writer, invoice, 132f, calculatorInvoice)
        y = drawTableHeader(writer, y, calculatorInvoice)
        invoice.items.forEachIndexed { index, item ->
            val rowHeight = if (calculatorInvoice) 30f else if (item.unit == SaleUnit.PACKAGE) 36f else 30f
            if (y + rowHeight > PAGE_HEIGHT - 130f) {
                writer.finishPage()
                writer.startPage()
                drawContinuationHeader(writer, invoice)
                y = drawTableHeader(writer, 78f, calculatorInvoice)
            }
            y = if (calculatorInvoice) {
                drawCalculatorItemRow(writer, item, index, y, rowHeight)
            } else {
                drawItemRow(writer, item, index, y, rowHeight)
            }
        }
        if (y + 90f > PAGE_HEIGHT - 40f) {
            writer.finishPage()
            writer.startPage()
            drawContinuationHeader(writer, invoice)
            y = 90f
        }
        y = drawTotals(writer, invoice, y + 12f)
        if (y + 55f < PAGE_HEIGHT - 30f) drawFooter(writer, company)
        writer.finish()

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${invoice.number}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun Invoice.isCalculatorQuickInvoice(): Boolean {
        if (items.isEmpty() || items.any { it.productId != null }) return false
        return items.all { item ->
            item.name == "مبلغ إجمالي سريع" || item.name.any { it in "+-×÷*/" }
        }
    }

    private fun drawHeader(writer: PageWriter, invoice: Invoice, company: CompanySettings, calculator: Boolean) {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        canvas.drawRoundRect(
            RectF(MARGIN, 28f, right, 104f), 12f, 12f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_DARK }
        )
        canvas.drawRoundRect(
            RectF(MARGIN, 28f, MARGIN + 7f, 104f), 4f, 4f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL }
        )

        val companyPaint = textPaint(15f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val detailPaint = textPaint(8.5f, 0xFFE0F2EF.toInt()).apply { textAlign = Paint.Align.RIGHT }
        val titlePaint = textPaint(17f, Color.WHITE, bold = true)
        val metaPaint = textPaint(8.5f, 0xFFE0F2EF.toInt())

        val companyMaxWidth = 250f
        canvas.drawText(ellipsize(company.name.ifBlank { "اسم شركتك" }, companyPaint, companyMaxWidth), right - 16f, 53f, companyPaint)
        if (company.phone.isNotBlank()) canvas.drawText(ellipsize("هاتف: ${company.phone}", detailPaint, companyMaxWidth), right - 16f, 72f, detailPaint)
        if (company.address.isNotBlank()) canvas.drawText(ellipsize("العنوان: ${company.address}", detailPaint, companyMaxWidth), right - 16f, 88f, detailPaint)

        canvas.drawText(if (calculator) "فاتورة حاسبة" else "فاتورة مبيعات", MARGIN + 22f, 54f, titlePaint)
        canvas.drawText("رقم الفاتورة: ${invoice.number}", MARGIN + 22f, 74f, metaPaint)
        canvas.drawText(Fmt.date(invoice.dateMillis), MARGIN + 22f, 90f, metaPaint)
    }

    private fun drawContinuationHeader(writer: PageWriter, invoice: Invoice) {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        canvas.drawRoundRect(RectF(MARGIN, 26f, right, 62f), 8f, 8f, Paint().apply { color = COLOR_TEAL_DARK })
        val paint = textPaint(10f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("فاتورة رقم ${invoice.number} - متابعة", right - 12f, 49f, paint)
    }

    private fun drawMeta(writer: PageWriter, invoice: Invoice, startY: Float, calculator: Boolean): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val boxTop = startY
        val boxBottom = startY + 72f
        canvas.drawRoundRect(
            RectF(left, boxTop, right, boxBottom), 9f, 9f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF5F8F8.toInt() }
        )
        val statusText = invoice.status.label
        val badgeColor = when (invoice.status) {
            InvoiceStatus.PAID -> COLOR_GREEN
            InvoiceStatus.UNPAID -> COLOR_RED
            InvoiceStatus.PARTIAL -> COLOR_AMBER
        }
        val badgeLeft = left + 12f
        val badgeTop = boxTop + 12f
        val badgePaint = textPaint(8f, Color.WHITE, bold = true)
        val badgeWidth = badgePaint.measureText(statusText) + 22f
        canvas.drawRoundRect(
            RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + 22f), 11f, 11f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor }
        )
        badgePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(statusText, badgeLeft + badgeWidth / 2f, badgeTop + 15f, badgePaint)

        val valuePaint = textPaint(9.5f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val detailPaint = textPaint(8.5f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val infoPaint = textPaint(8.5f, COLOR_MUTED)
        canvas.drawText(
            ellipsize("العميل: ${invoice.customerName.ifBlank { "عميل نقدي" }}", valuePaint, right - left - 28f),
            right - 12f, boxTop + 27f, valuePaint
        )
        canvas.drawText("الدفع: ${invoice.paymentMethod.label}", right - 12f, boxTop + 49f, detailPaint)
        canvas.drawText("عدد البنود: ${invoice.items.size}", left + 12f, boxTop + 52f, infoPaint)
        if (!calculator) canvas.drawText("إجمالي القطع: ${Fmt.qty(invoice.totalPieces)}", left + 12f, boxTop + 64f, infoPaint)
        if (invoice.customerPhone.isNotBlank()) {
            canvas.drawText(ellipsize("الهاتف: ${invoice.customerPhone}", detailPaint, 180f), right - 12f, boxTop + 64f, detailPaint)
        }
        return boxBottom + 18f
    }

    private val colName = 220f
    private val colQty = 92f
    private val colPrice = 90f
    private val colTotal = 113f

    private fun drawTableHeader(writer: PageWriter, top: Float, calculator: Boolean): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val height = 28f
        canvas.drawRoundRect(RectF(left, top, right, top + height), 5f, 5f, Paint().apply { color = COLOR_TEAL_DARK })
        val paint = textPaint(8.5f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.CENTER }
        val baseline = top + 18f
        if (calculator) {
            canvas.drawText("العملية الحسابية", right - 145f, baseline, paint)
            canvas.drawText("المبلغ", left + 55f, baseline, paint)
        } else {
            canvas.drawText("الصنف", right - colName / 2f, baseline, paint)
            canvas.drawText("الكمية", right - colName - colQty / 2f, baseline, paint)
            canvas.drawText("سعر الوحدة", right - colName - colQty - colPrice / 2f, baseline, paint)
            canvas.drawText("الإجمالي", left + colTotal / 2f, baseline, paint)
        }
        return top + height
    }

    private fun drawCalculatorItemRow(writer: PageWriter, item: InvoiceItem, index: Int, top: Float, height: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val bottom = top + height
        if (index % 2 == 1) canvas.drawRect(RectF(left, top, right, bottom), Paint().apply { color = COLOR_ROW_ALT })
        val namePaint = textPaint(9f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val amountPaint = textPaint(9.5f, COLOR_TEAL_DARK, bold = true).apply { textAlign = Paint.Align.CENTER }
        val name = if (item.name == "مبلغ إجمالي سريع") "مبلغ إجمالي سريع" else item.name
        canvas.drawText(ellipsize(name, namePaint, 320f), right - 12f, top + height / 2f + 3f, namePaint)
        canvas.drawText(Fmt.money(item.lineTotal), left + 55f, top + height / 2f + 3f, amountPaint)
        canvas.drawLine(left, bottom, right, bottom, Paint().apply { color = COLOR_LINE; strokeWidth = 0.8f })
        return bottom
    }

    private fun drawItemRow(writer: PageWriter, item: InvoiceItem, index: Int, top: Float, height: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val bottom = top + height
        if (index % 2 == 1) canvas.drawRect(RectF(left, top, right, bottom), Paint().apply { color = COLOR_ROW_ALT })

        val namePaint = textPaint(9f, COLOR_TEXT, bold = true)
        val subPaint = textPaint(7.5f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val cellPaint = textPaint(8.5f, COLOR_TEXT).apply { textAlign = Paint.Align.CENTER }
        val totalPaint = textPaint(9f, COLOR_TEAL_DARK, bold = true).apply { textAlign = Paint.Align.CENTER }
        val nameRight = right - 8f
        val capacity = item.unitsPerPackage.coerceAtLeast(1)
        val isPackage = item.unit == SaleUnit.PACKAGE
        namePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(ellipsize(item.name, namePaint, colName - 16f), nameRight, top + 13f, namePaint)
        if (isPackage) {
            canvas.drawText("${Fmt.qty(item.quantity)} حزمة × $capacity حبة", nameRight, top + 25f, subPaint)
        }

        val displayedQuantity = if (isPackage) item.totalPieces else item.quantity
        val displayedUnitPrice = if (isPackage) item.unitPrice / capacity.toDouble() else item.unitPrice
        canvas.drawText(Fmt.qty(displayedQuantity), right - colName - colQty / 2f, top + height / 2f + 3f, cellPaint)
        canvas.drawText(Fmt.amount(displayedUnitPrice), right - colName - colQty - colPrice / 2f, top + height / 2f + 3f, cellPaint)
        canvas.drawText(Fmt.amount(item.lineTotal), left + colTotal / 2f, top + height / 2f + 3f, totalPaint)
        canvas.drawLine(left, bottom, right, bottom, Paint().apply { color = COLOR_LINE; strokeWidth = 0.8f })
        return bottom
    }

    private fun drawTotals(writer: PageWriter, invoice: Invoice, startY: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        var y = startY + 8f
        val labelPaint = textPaint(9f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val valuePaint = textPaint(9.5f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("المجموع الفرعي", right - 100f, y, labelPaint)
        canvas.drawText(Fmt.money(invoice.subtotal), right - 8f, y, valuePaint)
        y += 17f
        if (invoice.discount > 0.0) {
            canvas.drawText("الخصم", right - 100f, y, labelPaint)
            canvas.drawText("- ${Fmt.money(invoice.discount)}", right - 8f, y, valuePaint)
            y += 17f
        }
        y += 4f
        val boxTop = y
        val boxBottom = y + 34f
        canvas.drawRoundRect(
            RectF(right - 260f, boxTop, right, boxBottom), 7f, 7f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_SOFT }
        )
        val finalLabel = textPaint(10f, COLOR_TEAL_DARK, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val finalValue = textPaint(13f, COLOR_TEAL, bold = true).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("الإجمالي النهائي", right - 12f, boxTop + 22f, finalLabel)
        canvas.drawText(Fmt.money(invoice.total), right - 150f, boxTop + 23f, finalValue)
        return boxBottom
    }

    private fun drawFooter(writer: PageWriter, company: CompanySettings) {
        val canvas = writer.canvas
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 62f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 62f, Paint().apply { color = COLOR_LINE; strokeWidth = 1f })
        val paint = textPaint(9f, COLOR_MUTED).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(company.footerNote.ifBlank { "شكراً لتعاملكم معنا" }, PAGE_WIDTH / 2f, PAGE_HEIGHT - 42f, paint)
        val small = textPaint(7.5f, COLOR_MUTED).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("تم إنشاء الفاتورة بواسطة فاتورة سريعة", PAGE_WIDTH / 2f, PAGE_HEIGHT - 27f, small)
    }

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

    private class PageWriter(private val document: PdfDocument) {
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        private var pageNumber = 0
        init { startPage() }
        fun startPage() {
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            canvas.drawColor(Color.WHITE)
        }
        fun finishPage() { document.finishPage(page) }
        fun finish() { finishPage() }
    }
}
