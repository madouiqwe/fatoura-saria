package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompanySettings

@Composable
fun CompanySettingsDialog(
    currentSettings: CompanySettings,
    onDismiss: () -> Unit,
    onSave: (CompanySettings) -> Unit
) {
    var name by remember { mutableStateOf(currentSettings.name) }
    var phone by remember { mutableStateOf(currentSettings.phone) }
    var address by remember { mutableStateOf(currentSettings.address) }
    var footer by remember { mutableStateOf(currentSettings.footerNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعدادات المؤسسة / الفاتورة",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "البيانات التي تظهر أعلى الفواتير المطبوعة والمُصدرة:",
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المؤسسة / المتجر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_company_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_company_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / المدينة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_company_address_input")
                )

                OutlinedTextField(
                    value = footer,
                    onValueChange = { footer = it },
                    label = { Text("عبارة الشكر / التذييل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_company_footer_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CompanySettings(
                            name = name.trim().ifBlank { "اسم شركتك" },
                            phone = phone.trim().ifBlank { "0555 55 55 55" },
                            address = address.trim().ifBlank { "العنوان" },
                            footerNote = footer.trim().ifBlank { "شكراً لتعاملكم معنا 🙏" }
                        )
                    )
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("حفظ التغييرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
