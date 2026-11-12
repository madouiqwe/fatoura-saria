package com.madoui.fatoura.print

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.madoui.fatoura.data.CompanySettings
import com.madoui.fatoura.data.DIRECT_AMOUNT_LABEL
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceItem
import com.madoui.fatoura.data.InvoiceStatus
import com.madoui.fatoura.data.SaleUnit
import com.madoui.fatoura.util.Fmt
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** إنشاء فاتورة A4 عصرية مع تمييز ملخص الحاسبة عن فاتورة الأصناف. */
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
    private const val COLOR_ACCENT = 0xFFF9A825.toInt()
    private const val COLOR_ROW_ALT = 0xFFF9FBFB.toInt()
    private const val COLOR_TILE = 0xFFF5F8F8.toInt()

    fun create(context: Context, invoice: Invoice, company: CompanySettings): File {
        val document = PdfDocument()
        val writer = PageWriter(document)
        val isCalculator = invoice.isCalculator
        drawHeader(writer, invoice, company, isCalculator)

        var y = drawMeta(writer, invoice, 166f, isCalculator)
        y = drawTableHeader(writer, y, isCalculator)
        invoice.items.forEachIndexed { index, item ->
            val rowHeight = when {
                isCalculator -> if (item.quantity > 1.0) 34f else 28f
                item.unit == SaleUnit.PACKAGE -> 34f
                else -> 28f
            }
            if (y + rowHeight > PAGE_HEIGHT - 150f) {
                writer.finishPage()
                writer.startPage()
                drawContinuationHeader(writer, invoice)
                y = drawTableHeader(writer, 84f, isCalculator)
            }
            y = if (isCalculator) drawCalculatorItemRow(writer, item, index, y, rowHeight)
            else drawItemRow(writer, item, index, y, rowHeight)
        }
        if (y + 120f > PAGE_HEIGHT - 60f) {
            writer.finishPage()
            writer.startPage()
            drawContinuationHeader(writer, invoice)
            y = 96f
        }
        y = drawTotals(writer, invoice, y + 14f)
        if (!isCalculator) drawSignatureArea(writer, y + 16f)
        drawFooter(writer, company)
        writer.finish()

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${invoice.number}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawHeader(writer: PageWriter, invoice: Invoice, company: CompanySettings, isCalculator: Boolean) {
        val canvas = writer.canvas
        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN
        val top = 32f
        val bottom = top + 124f

        canvas.drawRoundRect(
            RectF(left, top, right, bottom), 14f, 14f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    right, top, left, bottom,
                    COLOR_TEAL_DARK, COLOR_TEAL, Shader.TileMode.CLAMP
                )
            }
        )
        // شريط لوني رفيع أعلى البطاقة.
        canvas.drawRoundRect(
            RectF(left + 18f, top + 14f, left + 88f, top + 18f), 2f, 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT }
        )

        // شعار نصي بحرف المؤسسة.
        val logoBounds = RectF(right - 66f, top + 18f, right - 20f, top + 64f)
        canvas.drawRoundRect(logoBounds, 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        val logoText = company.name.trim().firstOrNull()?.toString() ?: "ف"
        val logoPaint = textPaint(19f, COLOR_TEAL_DARK, bold = true).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(logoText, logoBounds.centerX(), logoBounds.centerY() + 7f, logoPaint)

        val companyPaint = textPaint(15f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val detailPaint = textPaint(8.5f, 0xFFE0F2EF.toInt()).apply { textAlign = Paint.Align.RIGHT }
        val companyMaxWidth = right - left - 100f
        canvas.drawText(
            ellipsize(company.name.ifBlank { "اسم شركتك" }, companyPaint, companyMaxWidth),
            right - 74f, top + 40f, companyPaint
        )
        if (company.phone.isNotBlank()) {
            canvas.drawText(
                ellipsize("هاتف: ${company.phone}", detailPaint, companyMaxWidth),
                right - 74f, top + 58f, detailPaint
            )
        }
        if (company.address.isNotBlank()) {
            canvas.drawText(
                ellipsize(company.address, detailPaint, companyMaxWidth),
                right - 74f, top + 74f, detailPaint
            )
        }

        val titlePaint = textPaint(8.5f, 0xFFCDEAE4.toInt(), bold = true)
        val numberPaint = textPaint(23f, Color.WHITE, bold = true)
        val metaPaint = textPaint(8.5f, 0xFFE0F2EF.toInt())
        canvas.drawText(if (isCalculator) "إيصال حساب فوري" else "فاتورة مبيعات", left + 18f, top + 38f, titlePaint)
        canvas.drawText("#${invoice.number}", left + 18f, top + 66f, numberPaint)
        canvas.drawText(
            "${Fmt.date(invoice.dateMillis)}  •  ${SimpleDateFormat("HH:mm", Locale.US).format(Date(invoice.dateMillis))}",
            left + 18f, top + 86f, metaPaint
        )

        val statusText = invoice.status.label
        val badgeColor = when (invoice.status) {
            InvoiceStatus.PAID -> COLOR_GREEN
            InvoiceStatus.UNPAID -> COLOR_RED
            InvoiceStatus.PARTIAL -> COLOR_AMBER
        }
        val badgePaint = textPaint(8f, Color.WHITE, bold = true)
        val badgeWidth = badgePaint.measureText(statusText) + 20f
        val badgeBounds = RectF(left + 18f, top + 96f, left + 18f + badgeWidth, top + 114f)
        canvas.drawRoundRect(badgeBounds, 9f, 9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor })
        badgePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(statusText, badgeBounds.centerX(), badgeBounds.centerY() + 3.5f, badgePaint)
    }

    private fun drawContinuationHeader(writer: PageWriter, invoice: Invoice) {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        canvas.drawRoundRect(
            RectF(MARGIN, 30f, right, 66f), 8f, 8f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_DARK }
        )
        val paint = textPaint(10f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("فاتورة رقم ${invoice.number} • متابعة", right - 14f, 52f, paint)
    }

    private fun drawMeta(writer: PageWriter, invoice: Invoice, startY: Float, isCalculator: Boolean): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val tileWidth = (right - left - 14f) / 2f
        val height = 66f

        canvas.drawRoundRect(
            RectF(right - tileWidth, startY, right, startY + height), 10f, 10f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TILE }
        )
        canvas.drawRoundRect(
            RectF(left, startY, left + tileWidth, startY + height), 10f, 10f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TILE }
        )

        val labelPaint = textPaint(7.5f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val valuePaint = textPaint(10f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val hintPaint = textPaint(8f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("العميل", right - 12f, startY + 17f, labelPaint)
        canvas.drawText(
            ellipsize(invoice.customerName.ifBlank { "عميل نقدي" }, valuePaint, tileWidth - 24f),
            right - 12f, startY + 35f, valuePaint
        )
        canvas.drawText(
            invoice.customerPhone.ifBlank { "بدون هاتف" },
            right - 12f, startY + 52f, hintPaint
        )

        canvas.drawText("طريقة الدفع", left + tileWidth - 12f, startY + 17f, labelPaint)
        canvas.drawText(invoice.paymentMethod.label, left + tileWidth - 12f, startY + 35f, valuePaint)
        canvas.drawText(
            if (isCalculator) "${invoice.items.size} عملية حسابية"
            else "${invoice.items.size} صنف • ${Fmt.qty(invoice.totalPieces)} حبة",
            left + tileWidth - 12f, startY + 52f, hintPaint
        )

        return startY + height + 16f
    }

    private val colName = 220f
    private val colQty = 92f
    private val colPrice = 90f
    private val colTotal = 113f

    private fun drawTableHeader(writer: PageWriter, top: Float, isCalculator: Boolean): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val height = 28f
        canvas.drawRoundRect(
            RectF(left, top, right, top + height), 6f, 6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_DARK }
        )
        val paint = textPaint(8.5f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.CENTER }
        val baseline = top + 18f
        if (isCalculator) {
            canvas.drawText("العملية الحسابية", right - 135f, baseline, paint)
            canvas.drawText("المبلغ (د.ج)", left + 55f, baseline, paint)
        } else {
            canvas.drawText("الصنف / المنتج", right - colName / 2f, baseline, paint)
            canvas.drawText("الكمية (حبة)", right - colName - colQty / 2f, baseline, paint)
            canvas.drawText("سعر الحبة (د.ج)", right - colName - colQty - colPrice / 2f, baseline, paint)
            canvas.drawText("الإجمالي (د.ج)", left + colTotal / 2f, baseline, paint)
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
        val subPaint = textPaint(7.5f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val lineLabel = when (item.name.trim()) {
            "مبلغ إجمالي سريع", DIRECT_AMOUNT_LABEL, "" -> "مبلغ مباشر"
            else -> item.name
        }
        val hasBreakdown = item.quantity > 1.0
        val amountY = if (hasBreakdown) top + 15f else top + height / 2f + 3f
        canvas.drawText("${index + 1}. ${ellipsize(lineLabel, namePaint, 300f)}", right - 12f, amountY, namePaint)
        if (hasBreakdown) {
            canvas.drawText(
                "${Fmt.qty(item.quantity)} حبة × ${Fmt.amount(item.unitPrice)} د.ج",
                right - 12f,
                top + 26f,
                subPaint
            )
        }
        canvas.drawText(Fmt.money(item.lineTotal), left + 55f, amountY, amountPaint)
        canvas.drawLine(left, bottom, right, bottom, Paint().apply { color = COLOR_LINE; strokeWidth = 0.8f })
        return bottom
    }

    private fun drawItemRow(writer: PageWriter, item: InvoiceItem, index: Int, top: Float, height: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val bottom = top + height
        if (index % 2 == 1) canvas.drawRect(RectF(left, top, right, bottom), Paint().apply { color = COLOR_ROW_ALT })

        val namePaint = textPaint(9f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val subPaint = textPaint(7.5f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val cellPaint = textPaint(8.5f, COLOR_TEXT).apply { textAlign = Paint.Align.CENTER }
        val totalPaint = textPaint(9f, COLOR_TEAL_DARK, bold = true).apply { textAlign = Paint.Align.CENTER }
        val nameRight = right - 8f
        val capacity = item.unitsPerPackage.coerceAtLeast(1)
        val isPackage = item.unit == SaleUnit.PACKAGE
        canvas.drawText("${index + 1}. ${ellipsize(item.name, namePaint, colName - 24f)}", nameRight, top + 13f, namePaint)
        if (isPackage) {
            canvas.drawText("${Fmt.qty(item.quantity)} حزمة × $capacity حبة", nameRight, top + 25f, subPaint)
        }

        canvas.drawText("${Fmt.qty(item.totalPieces)}", right - colName - colQty / 2f, top + height / 2f + 3f, cellPaint)
        canvas.drawText(Fmt.amount(item.piecePrice), right - colName - colQty - colPrice / 2f, top + height / 2f + 3f, cellPaint)
        canvas.drawText(Fmt.amount(item.lineTotal), left + colTotal / 2f, top + height / 2f + 3f, totalPaint)
        canvas.drawLine(left, bottom, right, bottom, Paint().apply { color = COLOR_LINE; strokeWidth = 0.8f })
        return bottom
    }

    private fun drawTotals(writer: PageWriter, invoice: Invoice, startY: Float): Float {
        val canvas = writer.canvas
        val right = PAGE_WIDTH - MARGIN
        val left = MARGIN
        val cardWidth = 250f
        val cardLeft = right - cardWidth
        var y = startY + 18f
        val labelPaint = textPaint(8.5f, COLOR_MUTED).apply { textAlign = Paint.Align.RIGHT }
        val valuePaint = textPaint(9.5f, COLOR_TEXT, bold = true).apply { textAlign = Paint.Align.RIGHT }
        if (invoice.isCalculator) {
            canvas.drawText("عدد العمليات", right - 100f, y, labelPaint)
            canvas.drawText("${invoice.items.size}", right - 8f, y, valuePaint)
        } else {
            canvas.drawText("إجمالي عدد القطع", right - 100f, y, labelPaint)
            canvas.drawText("${Fmt.qty(invoice.totalPieces)} حبة", right - 8f, y, valuePaint)
        }
        y += 18f
        canvas.drawText("المجموع الفرعي", right - 100f, y, labelPaint)
        canvas.drawText(Fmt.money(invoice.subtotal), right - 8f, y, valuePaint)
        y += 18f
        if (invoice.discount > 0.0) {
            canvas.drawText("الخصم", right - 100f, y, labelPaint)
            canvas.drawText("- ${Fmt.money(invoice.discount)}", right - 8f, y, valuePaint)
            y += 18f
        }
        y += 6f
        val bandTop = y
        val bandBottom = y + 36f
        canvas.drawRoundRect(
            RectF(cardLeft, bandTop, right, bandBottom), 9f, 9f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL }
        )
        val bandLabel = textPaint(9.5f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.RIGHT }
        val bandValue = textPaint(14f, Color.WHITE, bold = true).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText("الإجمالي النهائي", right - 12f, bandTop + 23f, bandLabel)
        canvas.drawText(Fmt.money(invoice.total), cardLeft + 12f, bandTop + 24f, bandValue)

        if (invoice.notes.isNotBlank()) {
            val notesTop = startY + 18f
            val notesBottom = notesTop + 52f
            canvas.drawRoundRect(
                RectF(left, notesTop, left + 230f, notesBottom), 9f, 9f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TEAL_SOFT }
            )
            val notesLabel = textPaint(7.5f, COLOR_TEAL_DARK, bold = true).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("ملاحظات", left + 218f, notesTop + 16f, notesLabel)
            val notesPaint = textPaint(8f, COLOR_TEXT).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(
                ellipsize(invoice.notes, notesPaint, 206f),
                left + 218f, notesTop + 32f, notesPaint
            )
        }
        return bandBottom
    }

    private fun drawSignatureArea(writer: PageWriter, top: Float) {
        if (top + 54f > PAGE_HEIGHT - 70f) return
        val canvas = writer.canvas
        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_LINE; strokeWidth = 1f }
        canvas.drawRoundRect(
            RectF(left, top, right, top + 48f), 9f, 9f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TILE }
        )
        val label = textPaint(8.5f, COLOR_MUTED)
        label.textAlign = Paint.Align.CENTER
        canvas.drawText("توقيع العميل", left + 95f, top + 18f, label)
        canvas.drawLine(left + 28f, top + 36f, left + 162f, top + 36f, linePaint)
        canvas.drawText("ختم المؤسسة", right - 95f, top + 18f, label)
        canvas.drawLine(right - 162f, top + 36f, right - 28f, top + 36f, linePaint)
    }

    private fun drawFooter(writer: PageWriter, company: CompanySettings) {
        val canvas = writer.canvas
        canvas.drawLine(
            MARGIN, PAGE_HEIGHT - 58f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 58f,
            Paint().apply { color = COLOR_LINE; strokeWidth = 1f }
        )
        val paint = textPaint(9f, COLOR_MUTED).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(company.footerNote.ifBlank { "شكراً لتعاملكم معنا" }, PAGE_WIDTH / 2f, PAGE_HEIGHT - 40f, paint)
        val small = textPaint(7.5f, COLOR_MUTED).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("فاتورة سريعة", PAGE_WIDTH / 2f, PAGE_HEIGHT - 26f, small)
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
