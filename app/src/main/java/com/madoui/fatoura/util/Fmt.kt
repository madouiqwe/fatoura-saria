package com.madoui.fatoura.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** تنسيق موحّد للأرقام والتواريخ في كل التطبيق. */
object Fmt {

    private val moneyFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val qtyFormat = DecimalFormat("#,##0.###", DecimalFormatSymbols(Locale.US))
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US)

    @Synchronized
    fun money(value: Double): String = "${moneyFormat.format(value)} د.ج"

    @Synchronized
    fun amount(value: Double): String = moneyFormat.format(value)

    @Synchronized
    fun qty(value: Double): String = qtyFormat.format(value)

    @Synchronized
    fun date(millis: Long): String = dateFormat.format(Date(millis))

    @Synchronized
    fun dateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    /** يحوّل نص المستخدم (قد يحتوي فاصلة عربية) إلى رقم. */
    fun parseNumber(text: String): Double? {
        val normalized = text
            .replace('،', '.')
            .replace(',', '.')
            .replace(" ", "")
            .trim()
        return normalized.toDoubleOrNull()
    }
}
