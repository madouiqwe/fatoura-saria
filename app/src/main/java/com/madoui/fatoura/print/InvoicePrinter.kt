package com.madoui.fatoura.print

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.madoui.fatoura.data.CompanySettings
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.util.Fmt
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/** طباعة / حفظ PDF عبر نظام أندرويد، ومشاركة الملف. */
object InvoicePrinter {

    private const val THERMAL_PRINTER_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    // XP-P801 prints on 80mm paper, with an effective printable width of about 72mm (576 dots at 203dpi).
    private const val THERMAL_WIDTH_DOTS = 576

    fun print(context: Context, file: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (exception: Exception) {
                    callback.onWriteFailed(exception.message)
                }
            }
        }
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, adapter, attributes)
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
    }

    fun printDirectThermal(context: Context, invoice: Invoice, company: CompanySettings): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "يرجى السماح للتطبيق بالاتصال بأجهزة البلوتوث من إعدادات الأذونات.", Toast.LENGTH_LONG).show()
            return false
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(context, "لا توجد بطاقة بلوتوث متاحة في هذا الجهاز.", Toast.LENGTH_LONG).show()
            return false
        }

        val pairedDevices: List<BluetoothDevice> = try {
            adapter.bondedDevices.toList()
        } catch (_: SecurityException) {
            Toast.makeText(context, "لا توجد صلاحية للوصول إلى أجهزة البلوتوث المقترنة.", Toast.LENGTH_LONG).show()
            return false
        }
        val device = pairedDevices.firstOrNull {
            val name = it.name.orEmpty()
            name.contains("XP-P801", ignoreCase = true) || name.contains("P801", ignoreCase = true)
        } ?: pairedDevices.firstOrNull {
            it.name?.contains("printer", ignoreCase = true) == true
        } ?: pairedDevices.firstOrNull()

        if (device == null) {
            Toast.makeText(context, "لا توجد طابعات مقترنة. قم بإقران XP-P801 عبر إعدادات البلوتوث أولاً.", Toast.LENGTH_LONG).show()
            return false
        }

        return try {
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(THERMAL_PRINTER_UUID))
            socket.use { connection ->
                connection.connect()
                connection.outputStream.use { out ->
                    out.write(buildThermalText(invoice, company))
                    out.flush()
                }
            }
            Toast.makeText(context, "تم إرسال الفاتورة إلى الطابعة ${device.name ?: "XP-P801"}.", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "فشل الاتصال بالطابعة: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Render receipt text via Android's Arabic shaping/RTL engine and send a raster ESC/POS image.
     * This avoids dependence on the printer's built-in Arabic font/code-page support.
     */
    fun buildThermalText(invoice: Invoice, company: CompanySettings): ByteArray {
        val lines = buildList {
            add(company.name.ifBlank { "اسم الشركة" })
            add("هاتف: ${company.phone.ifBlank { "-" }}")
            add("العنوان: ${company.address.ifBlank { "-" }}")
            add("--------------------------------")
            add("فاتورة رقم: ${invoice.number}")
            add("التاريخ: ${Fmt.date(invoice.dateMillis)}")
            add("العميل: ${invoice.customerName.ifBlank { "عميل نقدي" }}")
            add("طريقة الدفع: ${invoice.paymentMethod.label}")
            add("--------------------------------")
            invoice.items.forEach { item ->
                add(item.name.ifBlank { "منتج" })
                add("${Fmt.qty(item.quantity)} ${item.unit.label} | ${Fmt.amount(item.unitPrice)} | ${Fmt.amount(item.lineTotal)}")
            }
            add("--------------------------------")
            add("المجموع الفرعي: ${Fmt.money(invoice.subtotal)}")
            if (invoice.discount > 0.0) add("الخصم: -${Fmt.money(invoice.discount)}")
            add("الإجمالي النهائي: ${Fmt.money(invoice.total)}")
            if (invoice.notes.isNotBlank()) add("ملاحظات: ${invoice.notes}")
            add("--------------------------------")
            add(company.footerNote.ifBlank { "شكراً لتعاملكم معنا" })
        }

        val receiptText = lines.joinToString("\n")
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 25f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        }
        val layout = StaticLayout.Builder.obtain(receiptText, 0, receiptText.length, textPaint, THERMAL_WIDTH_DOTS - 24)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(true)
            .setLineSpacing(3f, 1f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .build()

        val bitmapWidth = THERMAL_WIDTH_DOTS
        val bitmapHeight = layout.height + 24
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.translate(12f, 12f)
        layout.draw(canvas)
        canvas.restore()

        val widthBytes = (bitmapWidth + 7) / 8
        val payload = ByteArrayOutputStreamCompat()
        payload.write(byteArrayOf(0x1B, 0x40)) // ESC @ reset
        payload.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00)) // GS v 0 raster image
        payload.write(widthBytes and 0xFF)
        payload.write((widthBytes shr 8) and 0xFF)
        payload.write(bitmapHeight and 0xFF)
        payload.write((bitmapHeight shr 8) and 0xFF)

        val pixels = IntArray(bitmapWidth)
        for (y in 0 until bitmapHeight) {
            bitmap.getPixels(pixels, 0, bitmapWidth, 0, y, bitmapWidth, 1)
            for (byteIndex in 0 until widthBytes) {
                var value = 0
                for (bit in 0..7) {
                    val x = byteIndex * 8 + bit
                    if (x < bitmapWidth) {
                        val pixel = pixels[x]
                        val luminance = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
                        if (luminance < 160) value = value or (0x80 shr bit)
                    }
                }
                payload.write(value)
            }
        }
        payload.write(byteArrayOf(0x0A, 0x0A, 0x0A))
        bitmap.recycle()
        return payload.toByteArray()
    }

    private class ByteArrayOutputStreamCompat {
        private val buffer = java.io.ByteArrayOutputStream()
        fun write(value: Int) = buffer.write(value)
        fun write(bytes: ByteArray) = buffer.write(bytes)
        fun toByteArray(): ByteArray = buffer.toByteArray()
    }
}
