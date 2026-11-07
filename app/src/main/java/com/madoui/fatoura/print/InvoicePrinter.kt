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
import android.os.Handler
import android.os.Looper
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
import com.madoui.fatoura.data.SaleUnit
import com.madoui.fatoura.util.Fmt
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/** Android PDF printing/sharing and direct ESC/POS printing for XP-P801. */
object InvoicePrinter {

    private const val THERMAL_PRINTER_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    // XP-P801 80mm roll: approximately 72mm / 576 dots printable at 203dpi.
    private const val THERMAL_WIDTH_DOTS = 576
    private const val RASTER_BAND_HEIGHT = 256
    private val mainHandler = Handler(Looper.getMainLooper())

    fun print(context: Context, file: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?, destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?, callback: WriteResultCallback
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
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()
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

    /** Call on an IO dispatcher. Uses the paired SPP device and rasterized Android RTL text. */
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
        val device: BluetoothDevice? = try {
            adapter.bondedDevices.firstOrNull {
                val name = it.name.orEmpty()
                name.contains("XP-P801", true) || name.contains("P801", true)
            } ?: adapter.bondedDevices.firstOrNull { it.name?.contains("printer", true) == true }
        } catch (_: SecurityException) {
            showToast(context, "لا توجد صلاحية لقراءة أجهزة البلوتوث المقترنة.")
            return false
        }
        if (device == null) {
            showToast(context, "لم أعثر على XP-P801 مقترنة. أقرن الطابعة من إعدادات البلوتوث أولاً.")
            return false
        }

        var socket: BluetoothSocket? = null
        return try {
            // Cancel discovery before RFCOMM; scanning slows Bluetooth socket negotiation.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            ) {
                runCatching { adapter.cancelDiscovery() }
            }
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(THERMAL_PRINTER_UUID))
            socket.connect()
            socket.outputStream.use { output ->
                output.write(buildThermalText(invoice, company))
                output.flush()
            }
            showToast(context, "أُرسلت الفاتورة إلى ${device.name ?: "XP-P801"}.")
            true
        } catch (exception: Exception) {
            showToast(context, "تعذرت الطباعة. تحقق من تشغيل الطابعة واقترانها: ${exception.localizedMessage ?: "خطأ اتصال"}")
            false
        } finally {
            runCatching { socket?.close() }
        }
    }

    /** Build compact Arabic RTL receipt; package rows show piece price multiplied by pack count. */
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
            add("الصنف | الكمية | سعر الحبة | الإجمالي")
            add("────────────────────────────")
            invoice.items.forEach { item ->
                val name = item.name.ifBlank { "منتج" }
                if (item.unit == SaleUnit.PACKAGE) {
                    val capacity = item.unitsPerPackage.coerceAtLeast(1)
                    val piecePrice = if (capacity > 0) item.unitPrice / capacity.toDouble() else 0.0
                    add("$name | ${Fmt.qty(item.quantity)} حزمة | ${Fmt.amount(piecePrice)} | ${Fmt.amount(item.lineTotal)}")
                    add("= ${Fmt.qty(item.quantity)} × ${capacity} = ${Fmt.qty(item.totalPieces)} حبة")
                } else {
                    add("$name | ${Fmt.qty(item.quantity)} حبة | ${Fmt.amount(item.unitPrice)} | ${Fmt.amount(item.lineTotal)}")
                }
            }
            add("────────────────────────────")
            add("المجموع: ${Fmt.money(invoice.subtotal)}")
            if (invoice.discount > 0.0) add("الخصم: ${Fmt.money(invoice.discount)}")
            add("الإجمالي النهائي: ${Fmt.money(invoice.total)}")
            if (invoice.notes.isNotBlank()) add("ملاحظات: ${invoice.notes}")
            add("────────────────────────────")
            add(company.footerNote.ifBlank { "شكراً لتعاملكم معنا" })
        }
        val text = lines.joinToString("\n")
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 29f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, THERMAL_WIDTH_DOTS - 32)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(true)
            .setLineSpacing(2f, 1f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .build()

        val totalHeight = layout.height + 24
        val widthBytes = THERMAL_WIDTH_DOTS / 8
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(0x1B, 0x40)) // ESC @ initialize
        var top = 0
        while (top < totalHeight) {
            val height = minOf(RASTER_BAND_HEIGHT, totalHeight - top)
            val bitmap = Bitmap.createBitmap(THERMAL_WIDTH_DOTS, height, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                canvas.save()
                canvas.translate(16f, (12 - top).toFloat())
                layout.draw(canvas)
                canvas.restore()
                output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
                output.write(widthBytes and 0xFF)
                output.write((widthBytes shr 8) and 0xFF)
                output.write(height and 0xFF)
                output.write((height shr 8) and 0xFF)
                val pixels = IntArray(THERMAL_WIDTH_DOTS)
                for (y in 0 until height) {
                    bitmap.getPixels(pixels, 0, THERMAL_WIDTH_DOTS, 0, y, THERMAL_WIDTH_DOTS, 1)
                    for (byteIndex in 0 until widthBytes) {
                        var bits = 0
                        for (bit in 0..7) {
                            val color = pixels[byteIndex * 8 + bit]
                            val lum = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
                            if (lum < 160) bits = bits or (0x80 shr bit)
                        }
                        output.write(bits)
                    }
                }
            } finally {
                bitmap.recycle()
            }
            top += height
        }
        output.write(byteArrayOf(0x0A, 0x0A, 0x0A))
        return output.toByteArray()
    }

    private fun showToast(context: Context, message: String) {
        mainHandler.post { Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show() }
    }
}
