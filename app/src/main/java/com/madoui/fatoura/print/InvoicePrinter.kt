package com.madoui.fatoura.print

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
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
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
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
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
    }

    fun printDirectThermal(context: Context, invoice: Invoice, company: CompanySettings): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasConnect = context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            val hasScan = context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            if (!hasConnect || !hasScan) {
                Toast.makeText(
                    context,
                    "يجب منح أذونات البلوتوث (BLUETOOTH_CONNECT / BLUETOOTH_SCAN) أولاً.",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(context, "لا توجد بطاقة بلوتوث متاحة في هذا الجهاز.", Toast.LENGTH_LONG).show()
            return false
        }

        val pairedDevices: List<BluetoothDevice> = adapter.bondedDevices.toList()
        val device = pairedDevices
            .firstOrNull { it.name?.contains("printer", ignoreCase = true) == true }
            ?: pairedDevices.firstOrNull()

        if (device == null) {
            Toast.makeText(context, "لا توجد طابعات مقترنة. قم بإقران الطابعة أولاً.", Toast.LENGTH_LONG).show()
            return false
        }

        return try {
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(
                UUID.fromString(THERMAL_PRINTER_UUID)
            )
            socket.connect()
            val payload = buildThermalText(invoice, company)
            socket.outputStream.use { out ->
                out.write(payload)
                out.flush()
            }
            socket.close()
            Toast.makeText(context, "تم إرسال الفاتورة مباشرة إلى الطابعة.", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "فشل الاتصال بالطابعة: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    fun buildThermalText(invoice: Invoice, company: CompanySettings): ByteArray {
        val lines = mutableListOf<String>()
        val separator = "----------------------------------------"

        lines += "\u001B@"
        lines += company.name.ifBlank { "اسم الشركة" }
        lines += "هاتف: ${company.phone.ifBlank { "-" }}"
        lines += "العنوان: ${company.address.ifBlank { "-" }}"
        lines += separator
        lines += "فاتورة رقم: ${invoice.number}"
        lines += "التاريخ: ${Fmt.date(invoice.dateMillis)}"
        lines += "العميل: ${invoice.customerName.ifBlank { "عميل نقدي" }}"
        lines += "طريقة الدفع: ${invoice.paymentMethod.label}"
        lines += separator

        invoice.items.forEach { item ->
            val itemName = item.name.ifBlank { "منتج" }
            val qtyLabel = "${Fmt.qty(item.quantity)} ${item.unit.label}"
            val price = Fmt.amount(item.unitPrice)
            val total = Fmt.amount(item.lineTotal)
            lines += itemName
            lines += "$qtyLabel | $price | $total"
        }

        lines += separator
        lines += "المجموع الفرعي: ${Fmt.money(invoice.subtotal)}"
        if (invoice.discount > 0.0) {
            lines += "الخصم: -${Fmt.money(invoice.discount)}"
        }
        lines += "الإجمالي النهائي: ${Fmt.money(invoice.total)}"
        lines += "ملاحظات: ${invoice.notes.ifBlank { "-" }}"
        lines += separator
        lines += company.footerNote.ifBlank { "شكراً لتعاملكم معنا" }
        lines += ""

        return lines.joinToString("\n").toByteArray(Charsets.UTF_8)
    }
}
