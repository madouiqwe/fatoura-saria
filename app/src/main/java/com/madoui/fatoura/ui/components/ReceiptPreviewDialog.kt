package com.madoui.fatoura.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.madoui.fatoura.data.CompanySettings
import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.print.InvoicePrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * معاينة الإيصال الحراري قبل الطباعة: نفس الرسم الذي سيُرسل للطابعة،
 * فيرى المستخدم形状 الإيصال كما سيخرج على الورق تماماً.
 */
@Composable
fun ReceiptPreviewDialog(
    invoice: Invoice,
    settings: CompanySettings,
    isPrinting: Boolean,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(invoice.id, invoice.total, invoice.items.size) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(invoice.id, invoice.total, invoice.items.size) {
        val rendered = withContext(Dispatchers.IO) {
            runCatching { InvoicePrinter.renderReceiptPreview(context, invoice, settings) }.getOrNull()
        }
        if (rendered == null) failed = true else bitmap = rendered
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "معاينة الإيصال قبل الطباعة",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "هذا ما سيخرج من الطابعة حرفياً",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F3F5))
                        .padding(10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when {
                        failed -> Text(
                            "تعذّر تجهيز المعاينة، يمكنك الطباعة مباشرة.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )

                        bitmap == null -> CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color(0xFF00796B),
                            strokeWidth = 2.5.dp
                        )

                        else -> Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth,
                            contentDescription = "معاينة الإيصال"
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء", color = Color(0xFF6B7280)) }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onPrint,
                        enabled = !isPrinting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (isPrinting) "جارٍ..." else "طباعة",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
