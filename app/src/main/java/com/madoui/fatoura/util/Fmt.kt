package com.madoui.fatoura.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** تنسيق موحّد للأرقام والتواريخ في كل التطبيق. */
object Fmt {

    // لا تعرض فواصل الآلاف، ولا تعرض .00 عند عدم الحاجة.
    private val moneyFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))
    private val qtyFormat = DecimalFormat("0.###", DecimalFormatSymbols(Locale.US))
    private val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", Locale.US)
    private val dateTimeFormat = java.text.SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US)

    @Synchronized
    fun money(value: Double): String = "${moneyFormat.format(value)} د.ج"

    @Synchronized
    fun amount(value: Double): String = moneyFormat.format(value)

    @Synchronized
    fun qty(value: Double): String = qtyFormat.format(value)

    @Synchronized
    fun date(millis: Long): String = dateFormat.format(java.util.Date(millis))

    @Synchronized
    fun dateTime(millis: Long): String = dateTimeFormat.format(java.util.Date(millis))

    /** يحوّل نص المستخدم إلى رقم، مع حذف فواصل الآلاف والفواصل العشرية غير الصحيحة. */
    fun parseNumber(text: String): Double? {
        val clean = text
            .replace('،', '.')
            .replace(',', '.')
            .replace(" ", "")
            .trim()
            .removeSuffix(".")

        if (clean.isEmpty()) return null
        return clean.toDoubleOrNull()
    }
}
