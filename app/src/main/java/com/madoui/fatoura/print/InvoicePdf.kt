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

/** إنشاء فاتورة PDF بمقاس A4 مع دعم العربية. */
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
        y = drawTotals(writer, invoice, y + 14f)
        if (y + 60f < PAGE_HEIGHT - 40f) drawFooter(writer, company)
        writer.finish()

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${invoice.number}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawHeader(writer: PageWriter, invoice: Invoice, company: CompanySettings) {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val namePaint = textPaint(15f, COLOR_TEAL_DARK, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val nameMaxWidth = PAGE_WIDTH / 2f - MARGIN
        canvas.drawText(ellipsize(company.name.ifBlank { "اسم شركتك" }, namePaint, nameMaxWidth), right, 52f, namePaint)
        val small = textPaint(9f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(ellipsize("هاتف : ${company.phone}", small, nameMaxWidth), right, 70f, small)
        canvas.drawText(ellipsize("العنوان : ${company.address}", small, nameMaxWidth), right, 84f, small)
        canvas.drawText("فاتورة مبيعات", MARGIN, 52f, textPaint(19f, COLOR_TEAL, bold = true))
        val meta = textPaint(9f, COLOR_MUTED)
        canvas.drawText("رقم الفاتورة : ${invoice.number}", MARGIN, 72f, meta)
        canvas.drawText("التاريخ : ${Fmt.date(invoice.dateMillis)}", MARGIN, 86f, meta)
        canvas.drawLine(MARGIN, 100f, right, 100f, Paint().apply { color = COLOR_LINE; strokeWidth = 1f })
    }

    private fun drawMeta(writer: PageWriter, invoice: Invoice, startY: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val boxTop = startY
        val boxBottom = startY + 72f
        canvas.drawRoundRect(RectF(left, boxTop, right, boxBottom), 8f, 8f, Paint().apply { color = 0xFFF5F7F7.toInt() })
        val statusText = invoice.status.label
        val statusPaint = textPaint(8.5f, Color.WHITE, bold = true)
        val badgeLeft = left + 10f
        val badgeRight = badgeLeft + statusPaint.measureText(statusText) + 22f
        val badgeTop = boxTop + 9f
        val badgeBottom = badgeTop + 20f
        val badgeColor = when (invoice.status) {
            InvoiceStatus.PAID -> COLOR_GREEN
            InvoiceStatus.UNPAID -> COLOR_RED
            InvoiceStatus.PARTIAL -> COLOR_AMBER
        }
        canvas.drawRoundRect(RectF(badgeLeft, badgeTop, badgeRight, badgeBottom), 10f, 10f, Paint().apply { color = badgeColor })
        statusPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(statusText, (badgeLeft + badgeRight) / 2f, badgeBottom - 6f, statusPaint)

        val valuePaint = textPaint(9.5f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val detailPaint = textPaint(8f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val infoPaint = textPaint(8f, COLOR_MUTED).apply { textAlign = Paint.Align.LEFT }
        val textWidth = right - left - 20f
        canvas.drawText(ellipsize("العميل : ${invoice.customerName.ifBlank { "عميل نقدي" }}", valuePaint, textWidth), right, boxTop + 25f, valuePaint)
        canvas.drawText("طريقة الدفع : ${invoice.paymentMethod.label}", right, boxTop + 45f, detailPaint)
        canvas.drawText("عدد الأصناف : ${invoice.items.size}", left + 10f, boxTop + 52f, infoPaint)
        canvas.drawText("إجمالي القطع : ${Fmt.qty(invoice.totalPieces)}", left + 10f, boxTop + 64f, infoPaint)
        return boxBottom + 18f
    }

    private val colName = 220f
    private val colQty = 92f
    private val colPrice = 90f
    private val colTotal = 113f

    private fun drawTableHeader(writer: PageWriter, top: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val height = 26f
        canvas.drawRoundRect(RectF(left, top, right, top + height), 4f, 4f, Paint().apply { color = COLOR_TEAL_DARK })
        val paint = textPaint(9f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.CENTER }
        val baseline = top + 17f
        canvas.drawText("الصنف", right - colName / 2f, baseline, paint)
        canvas.drawText("الكمية (حبة)", right - colName - colQty / 2f, baseline, paint)
        canvas.drawText("سعر الحبة", right - colName - colQty - colPrice / 2f, baseline, paint)
        canvas.drawText("الإجمالي", left + colTotal / 2f, baseline, paint)
        return top + height
    }

    private fun drawItemRow(writer: PageWriter, item: InvoiceItem, index: Int, top: Float, height: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val bottom = top + height
        if (index % 2 == 1) canvas.drawRect(RectF(left, top, right, bottom), Paint().apply { color = COLOR_ROW_ALT })

        val namePaint = textPaint(9f, COLOR_TEXT, bold = true)
        val subPaint = textPaint(7f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val cellPaint = textPaint(8.5f, COLOR_TEXT).apply { textAlign = Paint.Align.CENTER }
        val totalPaint = textPaint(9f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.CENTER }
        val nameRight = right - 6f
        val capacity = item.unitsPerPackage.coerceAtLeast(1)
        val isPackage = item.unit == SaleUnit.PACKAGE
        val nameBaseline = if (isPackage) top + 13f else top + height / 2f + 3f
        namePaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(ellipsize(item.name, namePaint, colName - 14f), nameRight, nameBaseline, namePaint)
        if (isPackage) {
            canvas.drawText(
                "${Fmt.qty(item.quantity)} حزمة × $capacity حبة = ${Fmt.qty(item.totalPieces)} حبة",
                nameRight, top + 24f, subPaint
            )
        }

        // عند بيع الحزمة: اعرض عدد القطع الفعلي وسعر القطعة، مع بقاء الإجمالي محسوباً بسعر الحزمة.
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
        var y = startY + 10f
        val labelPaint = textPaint(9f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val valuePaint = textPaint(9.5f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.RIGHT }
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
        canvas.drawRoundRect(RectF(right - 250f, boxTop, right, boxBottom), 6f, 6f, Paint().apply { color = COLOR_TEAL_SOFT })
        val finalLabel = textPaint(10f, COLOR_TEAL_DARK, bold = true).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("الإجمالي النهائي", right - 10f, boxTop + 17f, finalLabel)
        val finalValue = textPaint(12f, COLOR_TEAL, bold = true).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(Fmt.money(invoice.total), right - 140f, boxTop + 18f, finalValue)
        return boxBottom
    }

    private fun drawFooter(writer: PageWriter, company: CompanySettings) {
        val canvas = writer.canvas
        val paint = textPaint(9f, COLOR_MUTED).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(company.footerNote.ifBlank { "شكراً لتعاملكم معنا" }, PAGE_WIDTH / 2f, PAGE_HEIGHT - 44f, paint)
        val small = textPaint(7.5f, COLOR_LINE).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("تم إنشاؤها بواسطة فاتورة سريعة v2.0", PAGE_WIDTH / 2f, PAGE_HEIGHT - 30f, small)
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
