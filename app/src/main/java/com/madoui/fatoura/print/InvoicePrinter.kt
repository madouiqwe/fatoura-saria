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
import android.os.Handler
import android.os.Looper
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/** Android PDF printing/sharing and direct ESC/POS printing for XP-P801. */
object InvoicePrinter {

    private const val THERMAL_PRINTER_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    // XP-P801: 80mm paper, approximately 72mm printable at 203dpi (576 dots).
    private const val THERMAL_WIDTH_DOTS = 576
    private const val RASTER_BAND_HEIGHT = 256
    private val mainHandler = Handler(Looper.getMainLooper())

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

    /**
     * Connects to an already paired XP-P801 over Bluetooth SPP and sends an ESC/POS raster receipt.
     * Arabic is shaped and laid out by Android, so printer firmware font/code-page support is not needed.
     * Call this function off the main thread. The function reports status using a main-thread Toast.
     */
    fun printDirectThermal(context: Context, invoice: Invoice, company: CompanySettings): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast(context, "يجب السماح للتطبيق بالاتصال بأجهزة البلوتوث.")
            return false
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            showToast(context, "لا توجد بطاقة بلوتوث في هذا الجهاز.")
            return false
        }

        val pairedDevices = try {
            adapter.bondedDevices.toList()
        } catch (_: SecurityException) {
            showToast(context, "لا توجد صلاحية لقراءة أجهزة البلوتوث المقترنة.")
            return false
        }
        val device: BluetoothDevice? = pairedDevices.firstOrNull {
            val name = it.name.orEmpty()
            name.contains("XP-P801", ignoreCase = true) || name.contains("P801", ignoreCase = true)
        } ?: pairedDevices.firstOrNull { it.name?.contains("printer", ignoreCase = true) == true }

        if (device == null) {
            showToast(context, "لم أعثر على XP-P801 مقترنة. أقرن الطابعة من إعدادات البلوتوث أولاً.")
            return false
        }

        return try {
            val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(THERMAL_PRINTER_UUID))
            socket.use { connection ->
                connection.connect()
                connection.outputStream.use { output ->
                    output.write(buildThermalText(invoice, company))
                    output.flush()
                }
            }
            showToast(context, "أُرسلت الفاتورة إلى ${device.name ?: "XP-P801"}.")
            true
        } catch (exception: Exception) {
            showToast(context, "تعذرت الطباعة. تحقق من تشغيل الطابعة واقترانها: ${exception.localizedMessage ?: "خطأ اتصال"}")
            false
        }
    }

    /** Builds a compact Arabic RTL receipt and rasterizes it in small bands for ESC/POS. */
    fun buildThermalText(invoice: Invoice, company: CompanySettings): ByteArray {
        val lines = buildList {
            add(company.name.ifBlank { "اسم الشركة" })
            if (company.phone.isNotBlank()) add("الهاتف: ${company.phone}")
            if (company.address.isNotBlank()) add("العنوان: ${company.address}")
            add("────────────────────────────")
            add("فاتورة: ${invoice.number}    ${Fmt.date(invoice.dateMillis)}")
            add("العميل: ${invoice.customerName.ifBlank { "عميل نقدي" }}")
            add("الدفع: ${invoice.paymentMethod.label}")
            add("────────────────────────────")
            invoice.items.forEach { item ->
                // Separate the name from values to avoid tiny, crowded multi-column Arabic text.
                add(item.name.ifBlank { "منتج" })
                val unitLabel = item.unit.label
                add("الكمية: ${Fmt.qty(item.quantity)} $unitLabel")
                add("سعر الوحدة: ${Fmt.amount(item.unitPrice)}    الإجمالي: ${Fmt.amount(item.lineTotal)}")
                if (item.unit == com.madoui.fatoura.data.SaleUnit.PACKAGE) {
                    add("عدد القطع: ${Fmt.qty(item.totalPieces)}")
                }
                add("")
            }
            add("────────────────────────────")
            add("المجموع: ${Fmt.money(invoice.subtotal)}")
            if (invoice.discount > 0.0) add("الخصم: ${Fmt.money(invoice.discount)}")
            add("الإجمالي النهائي: ${Fmt.money(invoice.total)}")
            if (invoice.notes.isNotBlank()) add("ملاحظات: ${invoice.notes}")
            add("────────────────────────────")
            add(company.footerNote.ifBlank { "شكراً لتعاملكم معنا" })
        }

        val receiptText = lines.joinToString("\n")
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 29f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val layout = StaticLayout.Builder.obtain(
            receiptText, 0, receiptText.length, paint, THERMAL_WIDTH_DOTS - 32
        )
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(true)
            .setLineSpacing(2f, 1f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .build()

        val totalHeight = layout.height + 24
        val widthBytes = THERMAL_WIDTH_DOTS / 8
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(0x1B, 0x40)) // Initialize printer.

        var top = 0
        while (top < totalHeight) {
            val bandHeight = minOf(RASTER_BAND_HEIGHT, totalHeight - top)
            val bitmap = Bitmap.createBitmap(THERMAL_WIDTH_DOTS, bandHeight, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                canvas.save()
                canvas.translate(16f, (12 - top).toFloat())
                layout.draw(canvas)
                canvas.restore()

                // ESC/POS GS v 0: raster bit image. Use bands to keep row height bounded.
                output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
                output.write(widthBytes and 0xFF)
                output.write((widthBytes shr 8) and 0xFF)
                output.write(bandHeight and 0xFF)
                output.write((bandHeight shr 8) and 0xFF)

                val rowPixels = IntArray(THERMAL_WIDTH_DOTS)
                for (y in 0 until bandHeight) {
                    bitmap.getPixels(rowPixels, 0, THERMAL_WIDTH_DOTS, 0, y, THERMAL_WIDTH_DOTS, 1)
                    for (byteIndex in 0 until widthBytes) {
                        var bits = 0
                        for (bit in 0..7) {
                            val color = rowPixels[byteIndex * 8 + bit]
                            val luminance = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
                            if (luminance < 160) bits = bits or (0x80 shr bit)
                        }
                        output.write(bits)
                    }
                }
            } finally {
                bitmap.recycle()
            }
            top += bandHeight
        }

        // Feed paper after receipt; avoid cutting because the XP-P801 variant may not have a cutter.
        output.write(byteArrayOf(0x0A, 0x0A, 0x0A))
        return output.toByteArray()
    }

    private fun showToast(context: Context, message: String) {
        mainHandler.post { Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show() }
    }
}
