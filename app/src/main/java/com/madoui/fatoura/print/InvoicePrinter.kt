package com.madoui.fatoura.print

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
import com.madoui.fatoura.data.DIRECT_AMOUNT_LABEL
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.SaleUnit
import com.madoui.fatoura.util.Fmt
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max

/** طباعة PDF عبر نظام أندرويد، وطباعة مباشرة ESC/POS لطابعة XP-P801. */
object InvoicePrinter {

    private const val THERMAL_PRINTER_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    // XP-P801 80mm roll: approximately 72mm / 576 dots printable at 203dpi.
    private const val THERMAL_WIDTH_DOTS = 576
    private const val SIDE_PADDING = 16
    private const val CONTENT_WIDTH = THERMAL_WIDTH_DOTS - SIDE_PADDING * 2
    private const val RASTER_BAND_HEIGHT = 256
    private val mainHandler = Handler(Looper.getMainLooper())

    /** أسماء شائعة لطابعات الإيصالات الحرارية المقترنة. */
    private val PRINTER_NAME_HINTS = listOf(
        "XP-P801", "P801", "XPRINTER", "XP-", "POS-", "MTP-", "GOOJPRT",
        "PT-", "THERMAL", "PRINTER", "58MM", "80MM", "طابعة"
    )

    private val clockFormat = SimpleDateFormat("HH:mm", Locale.US)

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

    /** يطبع الفاتورة مباشرة. يجب استدعاؤه من Dispatchers.IO. يعيد false عند أي فشل. */
    fun printDirectThermal(context: Context, invoice: Invoice, company: CompanySettings): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast(context, "يجب السماح للتطبيق بالاتصال بأجهزة البلوتوث.")
            return false
        }

        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            showToast(context, "لا توجد بطاقة بلوتوث في هذا الجهاز.")
            return false
        }
        if (!adapter.isEnabled) {
            showToast(context, "شغّل البلوتوث ثم أعد المحاولة.")
            return false
        }

        val paired = try {
            adapter.bondedDevices?.toList().orEmpty()
        } catch (_: SecurityException) {
            showToast(context, "لا توجد صلاحية لقراءة أجهزة البلوتوث المقترنة.")
            return false
        }

        val device = paired.firstOrNull { candidate ->
            val name = runCatching { candidate.name }.getOrNull().orEmpty()
            PRINTER_NAME_HINTS.any { name.contains(it, ignoreCase = true) }
        } ?: paired.singleOrNull()

        if (device == null) {
            val hint = if (paired.isEmpty()) {
                "لم أعثر على طابعة مقترنة. أقرن XP-P801 من إعدادات البلوتوث أولاً."
            } else {
                "اختر طابعة الإيصالات من إعدادات البلوتوث؛ يوجد ${paired.size} أجهزة مقترنة."
            }
            showToast(context, hint)
            return false
        }

        var socket: BluetoothSocket? = null
        return try {
            // Discovery slows RFCOMM negotiation down significantly on Android.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            ) {
                runCatching { adapter.cancelDiscovery() }
            }
            val payload = buildThermalText(invoice, company)
            socket = openSocket(device)
            socket.outputStream.use { output ->
                // نكتب على دفعات صغيرة: الطابعات الحرارية الرخيصة ذات المخزن الصغير
                // تُسقط البيانات إذا أُرسلت في دفعة واحدة كبيرة.
                val chunkSize = 2048
                var offset = 0
                while (offset < payload.size) {
                    val length = minOf(chunkSize, payload.size - offset)
                    output.write(payload, offset, length)
                    output.flush()
                    offset += length
                    Thread.sleep(10)
                }
                // انتظر حتى تفرغ الطابعة مخزنها قبل إغلاق الاتصال حتى لا يُقص الإيصال ناقصاً.
                val drainMs = (payload.size / 1024 * 20).coerceIn(700, 3500)
                Thread.sleep(drainMs.toLong())
            }
            showToast(context, "تم إرسال الفاتورة إلى ${runCatching { device.name }.getOrNull() ?: "XP-P801"}")
            true
        } catch (exception: Exception) {
            showToast(context, "تعذرت الطباعة: ${exception.localizedMessage ?: "خطأ اتصال"}")
            false
        } finally {
            runCatching { socket?.close() }
        }
    }

    /**
     * بعض نسخ XP-P801 ترفض القناة الآمنة (Secure RFCOMM)، لذا نجرّب
     * الاتصال الآمن ثم غير الآمن قبل إعلان الفشل.
     */
    private fun openSocket(device: BluetoothDevice): BluetoothSocket {
        val uuid = UUID.fromString(THERMAL_PRINTER_UUID)
        val secure = device.createRfcommSocketToServiceRecord(uuid)
        try {
            secure.connect()
            return secure
        } catch (secureFailure: Exception) {
            runCatching { secure.close() }
            val insecure = device.createInsecureRfcommSocketToServiceRecord(uuid)
            try {
                insecure.connect()
                return insecure
            } catch (insecureFailure: Exception) {
                runCatching { insecure.close() }
                throw insecureFailure
            }
        }
    }

    // ------------------------------------------------------------------ بناء الإيصال

    /** سطر إيصال جاهز للرسم: ارتفاعه ثابت ومحتواه يُرسم عند الإخراج. */
    private class ReceiptBlock(val height: Int, private val drawer: (Canvas, Int) -> Unit) {
        fun draw(canvas: Canvas, top: Int) = drawer(canvas, top)
    }

    private fun thermalPaint(size: Float, bold: Boolean, medium: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = size
            isSubpixelText = true
            typeface = when {
                bold -> Typeface.create("sans-serif", Typeface.BOLD)
                medium -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                else -> Typeface.create("sans-serif", Typeface.NORMAL)
            }
        }

    private fun buildLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        align: Layout.Alignment
    ): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(align)
        .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
        .setIncludePad(false)
        .setLineSpacing(2f, 1f)
        .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
        .build()

    private fun textBlock(
        text: String,
        size: Float = 22f,
        bold: Boolean = false,
        align: Layout.Alignment = Layout.Alignment.ALIGN_OPPOSITE,
        gap: Int = 3
    ): ReceiptBlock {
        val layout = buildLayout(text, thermalPaint(size, bold), CONTENT_WIDTH, align)
        return ReceiptBlock(layout.height + gap) { canvas, top ->
            canvas.save()
            canvas.translate(0f, top.toFloat())
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private fun centeredBlock(text: String, size: Float, bold: Boolean = false, gap: Int = 2): ReceiptBlock =
        textBlock(text, size = size, bold = bold, align = Layout.Alignment.ALIGN_CENTER, gap = gap)

    /** فاصل منقّط رفيع يمنح الإيصال مظهراً حديثاً بدل صفوف الشرطات. */
    private fun separatorBlock(gap: Int = 9, thickness: Float = 2f): ReceiptBlock {
        val height = (thickness + gap).toInt().coerceAtLeast(4)
        return ReceiptBlock(height) { canvas, top ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                strokeWidth = thickness
                strokeCap = Paint.Cap.ROUND
            }
            val y = top + thickness / 2f
            var x = 0f
            val end = CONTENT_WIDTH.toFloat()
            while (x < end) {
                canvas.drawLine(x, y, minOf(x + 7f, end), y, paint)
                x += 12f
            }
        }
    }

    /** سطر بعمودين: البيان على اليمين والمبلغ على اليسار، مع خط منقّط بينهما. */
    private fun rowBlock(
        label: String,
        value: String,
        size: Float = 22f,
        bold: Boolean = false,
        gap: Int = 3,
        leader: Boolean = true
    ): ReceiptBlock {
        val valuePaint = thermalPaint(size, bold)
        // المبالغ الكبيرة تأخذ مساحة أوسع، مع إبقاء مساحة كافية للبيان.
        val valueWidth = ceil(valuePaint.measureText(value)).toInt()
            .coerceIn(40, (CONTENT_WIDTH * 3) / 4)
        val labelWidth = (CONTENT_WIDTH - valueWidth - 8).coerceAtLeast(60)
        val labelLayout = buildLayout(label, thermalPaint(size, bold), labelWidth, Layout.Alignment.ALIGN_OPPOSITE)
        val valueLayout = buildLayout(value, valuePaint, valueWidth, Layout.Alignment.ALIGN_OPPOSITE)
        val height = max(labelLayout.height, valueLayout.height) + gap
        return ReceiptBlock(height) { canvas, top ->
            canvas.save()
            canvas.translate((CONTENT_WIDTH - labelWidth).toFloat(), top.toFloat())
            labelLayout.draw(canvas)
            canvas.restore()
            canvas.save()
            canvas.translate(0f, top.toFloat())
            valueLayout.draw(canvas)
            canvas.restore()
            if (leader && labelLayout.lineCount == 1 && valueLayout.lineCount == 1) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    strokeWidth = 2f
                    strokeCap = Paint.Cap.ROUND
                }
                val baseline = top + labelLayout.getLineBaseline(0) - 4f
                var x = valueWidth + 8f
                val end = CONTENT_WIDTH - labelWidth - 10f
                while (x < end) {
                    canvas.drawPoint(x, baseline, paint)
                    x += 9f
                }
            }
        }
    }

    /**
     * إطار الإجمالي النهائي: إطار مزدوج بخط أسود عريض ونص كبير.
     * لا نستخدم تعبئة سوداء لأن الطباعة الحرارية تخرجها غير متساوية على بعض الأجهزة.
     */
    private fun totalPanel(label: String, value: String): ReceiptBlock {
        val height = 70
        val labelPaint = thermalPaint(26f, true).apply { textAlign = Paint.Align.RIGHT }
        val valuePaint = thermalPaint(34f, true).apply { textAlign = Paint.Align.LEFT }
        val thin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val thick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        return ReceiptBlock(height + 12) { canvas, top ->
            val outer = RectF(3f, top + 3f, CONTENT_WIDTH - 3f, top + height - 3f)
            canvas.drawRoundRect(outer, 10f, 10f, thin)
            val inner = RectF(10f, top + 10f, CONTENT_WIDTH - 10f, top + height - 10f)
            canvas.drawRoundRect(inner, 6f, 6f, thick)
            canvas.drawText(label, CONTENT_WIDTH - 24f, top + 46f, labelPaint)
            canvas.drawText(value, 24f, top + 49f, valuePaint)
        }
    }

    /** عنوان قسم داخل إطار أسود رفيع يفصل أجزاء الإيصال بوضوح ودون تعبئة سوداء. */
    private fun sectionBar(text: String): ReceiptBlock {
        val height = 46
        val paint = thermalPaint(26f, true).apply { textAlign = Paint.Align.CENTER }
        val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        return ReceiptBlock(height + 6) { canvas, top ->
            canvas.drawRoundRect(
                RectF(2f, top + 2f, CONTENT_WIDTH - 2f, top + height - 2f),
                8f, 8f, frame
            )
            canvas.drawText(text, CONTENT_WIDTH / 2f, top + 32f, paint)
        }
    }

    /** يبني أمر ESC/POS كامل بصورة نقطية عالية الدقة (يدعم العربية بالكامل). */
    fun buildThermalText(invoice: Invoice, company: CompanySettings): ByteArray {
        val blocks = mutableListOf<ReceiptBlock>()
        val calculator = invoice.isCalculator

        blocks += textBlock(
            company.name.ifBlank { "اسم الشركة" },
            size = 44f,
            bold = true,
            align = Layout.Alignment.ALIGN_CENTER,
            gap = 4
        )
        if (company.phone.isNotBlank()) blocks += centeredBlock("هاتف: ${company.phone}", size = 24f, gap = 2)
        if (company.address.isNotBlank()) blocks += centeredBlock(company.address, size = 24f, gap = 3)
        blocks += separatorBlock(gap = 6, thickness = 3f)

        blocks += rowBlock("رقم الفاتورة", invoice.number, size = 34f, bold = true, gap = 6)
        blocks += rowBlock("التاريخ", Fmt.date(invoice.dateMillis), size = 24f, gap = 4)
        blocks += rowBlock("الوقت", clockFormat.format(Date(invoice.dateMillis)), size = 24f, gap = 5)
        blocks += rowBlock("العميل", invoice.customerName.ifBlank { "عميل نقدي" }, size = 26f, gap = 4)
        blocks += rowBlock("الدفع", invoice.paymentMethod.label, size = 24f, gap = 7)

        blocks += sectionBar(if (calculator) "إيصال حساب فوري" else "فاتورة مبيعات")

        if (calculator) {
            invoice.items.forEachIndexed { index, item ->
                val label = when (item.name.trim()) {
                    "مبلغ إجمالي سريع", DIRECT_AMOUNT_LABEL, "" -> "مبلغ مباشر"
                    else -> item.name
                }
                blocks += rowBlock("${index + 1}. $label", Fmt.money(item.lineTotal), size = 30f, bold = true, gap = 3)
                if (item.quantity > 1.0) {
                    blocks += textBlock(
                        "${Fmt.qty(item.quantity)} حبة × ${Fmt.amount(item.unitPrice)} د.ج",
                        size = 22f,
                        gap = 8
                    )
                }
            }
        } else {
            blocks += rowBlock("البيان", "الإجمالي", size = 22f, bold = true, leader = false, gap = 5)
            blocks += separatorBlock(gap = 7, thickness = 2f)
            invoice.items.forEachIndexed { index, item ->
                val capacity = item.unitsPerPackage.coerceAtLeast(1)
                blocks += textBlock("${index + 1}. ${item.name.ifBlank { "منتج" }}", size = 28f, bold = true, gap = 2)
                val quantityText = if (item.unit == SaleUnit.PACKAGE) {
                    "${Fmt.qty(item.quantity)} حزمة × $capacity = ${Fmt.qty(item.totalPieces)} حبة"
                } else {
                    "${Fmt.qty(item.quantity)} حبة"
                }
                blocks += textBlock("$quantityText  ×  ${Fmt.amount(item.piecePrice)} د.ج للحبة", size = 22f, gap = 3)
                blocks += rowBlock("الإجمالي", Fmt.money(item.lineTotal), size = 26f, bold = true, gap = 8)
            }
        }

        blocks += separatorBlock(gap = 7, thickness = 2f)
        blocks += rowBlock("عدد البنود", invoice.items.size.toString(), size = 23f, gap = 4)
        if (!calculator) {
            blocks += rowBlock("إجمالي الحبات", "${Fmt.qty(invoice.totalPieces)} حبة", size = 23f, gap = 4)
        }
        if (invoice.discount > 0.0) {
            blocks += rowBlock("المجموع الفرعي", Fmt.money(invoice.subtotal), size = 24f, gap = 4)
            blocks += rowBlock("الخصم", "- ${Fmt.money(invoice.discount)}", size = 24f, gap = 5)
        }
        blocks += totalPanel("الإجمالي", Fmt.money(invoice.total))

        if (invoice.notes.isNotBlank()) {
            blocks += textBlock("ملاحظات: ${invoice.notes}", size = 22f, gap = 10)
        }
        blocks += separatorBlock(gap = 8, thickness = 2f)
        blocks += centeredBlock(
            company.footerNote.ifBlank { "شكراً لتعاملكم معنا" },
            size = 26f,
            bold = true,
            gap = 5
        )
        blocks += centeredBlock("فاتورة سريعة • ${invoice.number}", size = 19f, gap = 5)

        val bitmap = renderReceipt(blocks)
        return try {
            encodeRaster(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun renderReceipt(blocks: List<ReceiptBlock>): Bitmap {
        val headerPadding = 10
        val height = blocks.sumOf { it.height } + headerPadding * 2
        val bitmap = Bitmap.createBitmap(THERMAL_WIDTH_DOTS, height.coerceAtLeast(64), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var top = headerPadding
        blocks.forEach { block ->
            canvas.save()
            canvas.translate(SIDE_PADDING.toFloat(), 0f)
            block.draw(canvas, top)
            canvas.restore()
            top += block.height
        }
        return bitmap
    }

    /** GS v 0 raster bit image في نطاقات محددة لتفادي أوامر ضخمة على البلوتوث. */
    private fun encodeRaster(bitmap: Bitmap): ByteArray {
        val widthBytes = THERMAL_WIDTH_DOTS / 8
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(0x1B, 0x40)) // ESC @  : تهيئة الطابعة
        output.write(byteArrayOf(0x1B, 0x33, 0x00)) // ESC 3 0: تباعد أسطر صفري للصورة
        val pixels = IntArray(THERMAL_WIDTH_DOTS)
        var top = 0
        while (top < bitmap.height) {
            val height = minOf(RASTER_BAND_HEIGHT, bitmap.height - top)
            output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            output.write(widthBytes and 0xFF)
            output.write((widthBytes shr 8) and 0xFF)
            output.write(height and 0xFF)
            output.write((height shr 8) and 0xFF)
            for (y in top until top + height) {
                bitmap.getPixels(pixels, 0, THERMAL_WIDTH_DOTS, 0, y, THERMAL_WIDTH_DOTS, 1)
                for (byteIndex in 0 until widthBytes) {
                    var bits = 0
                    for (bit in 0..7) {
                        val color = pixels[byteIndex * 8 + bit]
                        val luminance =
                            (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000
                        if (luminance < 176) bits = bits or (0x80 shr bit)
                    }
                    output.write(bits)
                }
            }
            top += height
        }
        // ESC 2 يعيد تباعد الأسطر الافتراضي قبل تغذية الورق (ESC 3 0 كان يصفّره للصورة).
        output.write(byteArrayOf(0x1B, 0x32))
        output.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
        output.write(byteArrayOf(0x1D, 0x56, 0x42, 0x03)) // GS V B 3: قص الورق
        return output.toByteArray()
    }

    private fun showToast(context: Context, message: String) {
        mainHandler.post { Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show() }
    }
}

