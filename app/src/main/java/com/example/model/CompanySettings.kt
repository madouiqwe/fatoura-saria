package com.example.model

import android.content.Context
import android.content.SharedPreferences

data class CompanySettings(
    val name: String = "اسم شركتك",
    val phone: String = "0555 55 55 55",
    val address: String = "العنوان",
    val footerNote: String = "شكراً لتعاملكم معنا 🙏"
) {
    companion object {
        private const val PREFS_NAME = "company_settings_prefs"
        private const val KEY_NAME = "name"
        private const val KEY_PHONE = "phone"
        private const val KEY_ADDRESS = "address"
        private const val KEY_FOOTER = "footer"

        fun load(context: Context): CompanySettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return CompanySettings(
                name = prefs.getString(KEY_NAME, "اسم شركتك") ?: "اسم شركتك",
                phone = prefs.getString(KEY_PHONE, "0555 55 55 55") ?: "0555 55 55 55",
                address = prefs.getString(KEY_ADDRESS, "العنوان") ?: "العنوان",
                footerNote = prefs.getString(KEY_FOOTER, "شكراً لتعاملكم معنا 🙏") ?: "شكراً لتعاملكم معنا 🙏"
            )
        }

        fun save(context: Context, settings: CompanySettings) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_NAME, settings.name)
                .putString(KEY_PHONE, settings.phone)
                .putString(KEY_ADDRESS, settings.address)
                .putString(KEY_FOOTER, settings.footerNote)
                .apply()
        }
    }
}
