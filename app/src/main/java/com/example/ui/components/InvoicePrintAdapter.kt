package com.example.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.model.CompanySettings
import com.example.model.Invoice
import com.example.model.SaleUnit
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoicePrintAdapter(
    private val context: Context,
    private val invoice: Invoice,
    private val companySettings: CompanySettings
) : PrintDocumentAdapter() {

    private var pdfDocument: PdfDocument? = null
    private val pageWidth = 595 // Standard A4 width in points
    private val pageHeight = 842 // Standard A4 height in points

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("فاتورة_${invoice.invoiceNumber}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument!!.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoice(canvas)

        pdfDocument!!.finishPage(page)

        try {
            destination?.fileDescriptor?.let { fd ->
                FileOutputStream(fd).use { out ->
                    pdfDocument!!.writeTo(out)
                }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback?.onWriteFailed(e.message)
        } finally {
            pdfDocument?.close()
            pdfDocument = null
        }
    }

    private fun drawInvoice(canvas: Canvas) {
        val margin = 36f
        var currentY = 48f

        val titlePaint = Paint().apply {
            color = Color.parseColor("#005B52")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.parseColor("#1A202C")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#4A5568")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val rightAlignPaint = Paint().apply {
            color = Color.parseColor("#1A202C")
            textSize = 10.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val rightAlignBoldPaint = Paint().apply {
            color = Color.parseColor("#005B52")
            textSize = 11.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Header - Right side: Invoice Title, Number, Date
        val rightX = pageWidth - margin
        canvas.drawText("فاتورة مبيعات", rightX, currentY, rightAlignBoldPaint.apply { textSize = 20f })
        currentY += 20f
        canvas.drawText("رقم الفاتورة : ${invoice.invoiceNumber}", rightX, currentY, rightAlignPaint)
        currentY += 16f
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        canvas.drawText("التاريخ : ${sdf.format(Date(invoice.issueDate))}", rightX, currentY, rightAlignPaint)

        // Header - Left side: Company Details
        var leftY = 48f
        canvas.drawText(companySettings.name, margin, leftY, titlePaint.apply { textSize = 16f })
        leftY += 18f
        canvas.drawText("هاتف : ${companySettings.phone}", margin, leftY, textPaint)
        leftY += 16f
        canvas.drawText("العنوان : ${companySettings.address}", margin, leftY, textPaint)

        currentY = maxOf(currentY, leftY) + 26f

        // Customer & Status Card Banner
        val bannerRect = RectF(margin, currentY, pageWidth - margin, currentY + 38f)
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(bannerRect, 6f, 6f, bannerPaint)

        // Customer Name
        val customerText = "العميل : ${invoice.customerName.ifBlank { "عميل نقدي" }}"
        canvas.drawText(customerText, margin + 12f, currentY + 23f, boldPaint.apply { textSize = 11f })

        // Payment Method
        val paymentText = "طريقة الدفع : ${invoice.paymentMethod.labelAr}"
        canvas.drawText(paymentText, margin + 220f, currentY + 23f, textPaint)

        // Status Badge (Right aligned)
        val statusBgPaint = Paint().apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val statusText = "حالة الفاتورة: ${invoice.status.labelAr}"
        val statusTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val badgeWidth = statusTextPaint.measureText(statusText) + 16f
        val badgeRect = RectF(pageWidth - margin - badgeWidth - 10f, currentY + 8f, pageWidth - margin - 10f, currentY + 30f)
        canvas.drawRoundRect(badgeRect, 4f, 4f, statusBgPaint)
        canvas.drawText(statusText, badgeRect.left + 8f, badgeRect.top + 15f, statusTextPaint)

        currentY += 52f

        // Table Header
        val colTotal = pageWidth - margin - 12f
        val colPrice = colTotal - 90f
        val colQty = colPrice - 90f
        val colItem = margin + 12f

        val tableHeaderRect = RectF(margin, currentY, pageWidth - margin, currentY + 26f)
        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#005B52")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(tableHeaderRect, 4f, 4f, tableHeaderPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerTextRightPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("الصنف", colItem, currentY + 17f, headerTextPaint)
        canvas.drawText("الكمية", colQty, currentY + 17f, headerTextPaint)
        canvas.drawText("السعر", colPrice, currentY + 17f, headerTextPaint)
        canvas.drawText("الإجمالي", colTotal, currentY + 17f, headerTextRightPaint)

        currentY += 26f

        // Table Rows
        val rowBgEven = Color.parseColor("#FFFFFF")
        val rowBgOdd = Color.parseColor("#F8FAFC")
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 0.8f
        }

        invoice.items.forEachIndexed { index, item ->
            val rowY = currentY
            val rowHeight = 28f

            val rowBgPaint = Paint().apply {
                color = if (index % 2 == 0) rowBgEven else rowBgOdd
                style = Paint.Style.FILL
            }
            canvas.drawRect(RectF(margin, rowY, pageWidth - margin, rowY + rowHeight), rowBgPaint)

            val itemTitle = if (item.saleUnit == SaleUnit.PACKAGE) {
                "${item.name} (${item.packagesCount.toInt().coerceAtLeast(1)} حزمة)"
            } else {
                "${item.name} (حبة)"
            }
            canvas.drawText(itemTitle, colItem, rowY + 18f, boldPaint.apply { textSize = 10f })

            val qtyText = if (item.saleUnit == SaleUnit.PACKAGE) {
                "${item.effectiveQuantity}"
            } else {
                "${item.quantity}"
            }
            canvas.drawText(qtyText, colQty, rowY + 18f, textPaint)

            canvas.drawText(formatCurrency(item.unitPrice), colPrice, rowY + 18f, textPaint)

            canvas.drawText(formatCurrency(item.total), colTotal, rowY + 18f, rightAlignBoldPaint.apply { textSize = 10.5f })

            canvas.drawLine(margin, rowY + rowHeight, pageWidth - margin, rowY + rowHeight, linePaint)
            currentY += rowHeight
        }

        currentY += 24f

        // Totals Box (Right aligned)
        val totalBoxRight = pageWidth - margin
        canvas.drawText("المجموع الفرعي : ${formatCurrency(invoice.subtotal)}", totalBoxRight, currentY, rightAlignPaint.apply { textSize = 12f })
        currentY += 20f

        if (invoice.calculatedDiscount > 0) {
            canvas.drawText("الخصم : -${formatCurrency(invoice.calculatedDiscount)}", totalBoxRight, currentY, rightAlignPaint.apply {
                color = Color.parseColor("#DC2626")
            })
            currentY += 20f
        }

        val grandTotalPaint = Paint().apply {
            color = Color.parseColor("#005B52")
            textSize = 15f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("الإجمالي النهائي : ${formatCurrency(invoice.grandTotal)}", totalBoxRight, currentY, grandTotalPaint)

        currentY += 50f

        // Footer Note
        val footerPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 12f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val note = companySettings.footerNote.ifBlank { "شكراً لتعاملكم معنا 🙏" }
        canvas.drawText(note, pageWidth / 2f, currentY, footerPaint)
    }

    companion object {
        fun printInvoice(context: Context, invoice: Invoice, companySettings: CompanySettings) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val jobName = "فاتورة_${invoice.invoiceNumber}"
            printManager?.print(jobName, InvoicePrintAdapter(context, invoice, companySettings), null)
        }
    }
}
