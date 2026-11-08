package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.Product
import com.example.model.SaleUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("فاتورة سريعة", appName)
  }

  @Test
  fun `invoice calculation and post completion edit verification`() {
    val item1 = InvoiceItem(name = "استشارة تقنية", quantity = 2.0, unitPrice = 500.0, saleUnit = SaleUnit.PIECE)
    val item2 = InvoiceItem(name = "تطوير لوحة تحكم", quantity = 1.0, unitPrice = 2000.0, saleUnit = SaleUnit.PIECE)

    val completedInvoice = Invoice(
      invoiceNumber = "INV-TEST-01",
      customerName = "عميل تجريبي",
      status = InvoiceStatus.COMPLETED,
      items = listOf(item1, item2),
      discountAmount = 100.0,
      isDiscountPercentage = false,
      taxPercentage = 15.0,
      paidAmount = 3335.0
    )

    // Subtotal: 1000 + 2000 = 3000
    assertEquals(3000.0, completedInvoice.subtotal, 0.01)
    // After discount: 3000 - 100 = 2900
    // Tax 15%: 2900 * 0.15 = 435
    // Grand Total: 2900 + 435 = 3335
    assertEquals(3335.0, completedInvoice.grandTotal, 0.01)
    assertEquals(0.0, completedInvoice.remainingBalance, 0.01)

    // User edits the completed invoice and adds another item:
    val item3 = InvoiceItem(name = "دعم فني إضافي", quantity = 1.0, unitPrice = 500.0, saleUnit = SaleUnit.PIECE)
    val editedInvoice = completedInvoice.copy(
      items = completedInvoice.items + item3,
      isEditedAfterCompletion = true
    )

    // Subtotal: 3000 + 500 = 3500
    // After discount: 3500 - 100 = 3400
    // Tax 15%: 3400 * 0.15 = 510
    // Grand Total: 3400 + 510 = 3910
    assertEquals(3500.0, editedInvoice.subtotal, 0.01)
    assertEquals(3910.0, editedInvoice.grandTotal, 0.01)
    assertTrue(editedInvoice.isEditedAfterCompletion)
    // Remaining balance updated automatically
    assertEquals(3910.0 - 3335.0, editedInvoice.remainingBalance, 0.01)
  }

  @Test
  fun `variable package capacities per product verification - 24 units and 10 units`() {
    // منتج 1: عصير برتقال - سعة الحزمة 24 حبة
    val juiceProduct = Product(
      id = "p-juice",
      name = "عصير برتقال 250 مل",
      defaultUnitPrice = 2.50,
      unitsPerPackage = 24.0, // سعة الحزمة 24
      packageTypeName = "كرتونة"
    )
    assertEquals(24.0, juiceProduct.unitsPerPackage, 0.001)

    // إضافة 3 كراتين عصير للفاتورة:
    // سعر الحبة 2.5 × سعة الحزمة 24 × 3 كراتين = 180.0 ر.س (إجمالي 72 قطعة)
    val juiceItem = InvoiceItem(
      name = juiceProduct.name,
      unitsPerPackage = juiceProduct.unitsPerPackage,
      packagesCount = 3.0,
      saleUnit = SaleUnit.PACKAGE,
      unitPrice = juiceProduct.defaultUnitPrice
    )
    assertEquals(72.0, juiceItem.quantity, 0.001) // 3 * 24 = 72
    assertEquals(60.0, juiceItem.packagePrice, 0.001) // 2.5 * 24 = 60.0
    assertEquals(180.0, juiceItem.total, 0.001) // 3 * 60.0 = 180.0

    // منتج 2: معجون طماطم - سعة الحزمة 10 حبات (ليست 12 ولا 24)
    val tomatoProduct = Product(
      id = "p-tomato",
      name = "معجون طماطم 135 جم",
      defaultUnitPrice = 3.00,
      unitsPerPackage = 10.0, // سعة الحزمة 10
      packageTypeName = "شد"
    )
    assertEquals(10.0, tomatoProduct.unitsPerPackage, 0.001)

    // إضافة 4 شدود معجون طماطم للفاتورة:
    // سعر الحبة 3.0 × سعة الحزمة 10 × 4 شدود = 120.0 ر.س (إجمالي 40 قطعة)
    val tomatoItem = InvoiceItem(
      name = tomatoProduct.name,
      unitsPerPackage = tomatoProduct.unitsPerPackage,
      packagesCount = 4.0,
      saleUnit = SaleUnit.PACKAGE,
      unitPrice = tomatoProduct.defaultUnitPrice
    )
    assertEquals(40.0, tomatoItem.quantity, 0.001) // 4 * 10 = 40
    assertEquals(30.0, tomatoItem.packagePrice, 0.001) // 3.0 * 10 = 30.0
    assertEquals(120.0, tomatoItem.total, 0.001) // 4 * 30.0 = 120.0

    val invoice = Invoice(
      invoiceNumber = "INV-VAR-01",
      customerName = "متجر الخير",
      items = listOf(juiceItem, tomatoItem),
      taxPercentage = 15.0
    )

    // Subtotal: 180 + 120 = 300.0
    assertEquals(300.0, invoice.subtotal, 0.001)
    // Tax 15%: 300 * 0.15 = 45.0
    assertEquals(45.0, invoice.calculatedTax, 0.001)
    // Grand Total: 345.0
    assertEquals(345.0, invoice.grandTotal, 0.001)
  }

  @Test
  fun `flexible unit selection - single piece sale with product package capacity retained`() {
    // بيع بالحبة المفردة لصنف سعة حزمته 24
    val singlePieceSale = InvoiceItem(
      name = "عصير برتقال 250 مل",
      unitsPerPackage = 24.0, // سعة الحزمة محددة بـ 24
      packagesCount = 0.0,
      singlePiecesCount = 5.0, // اشترى 5 حبات مفردة
      saleUnit = SaleUnit.PIECE,
      quantity = 5.0,
      unitPrice = 2.50
    )

    // الحساب بالحبة المفردة: 5 × 2.5 = 12.50 ر.س
    assertEquals(5.0, singlePieceSale.quantity, 0.001)
    assertEquals(12.50, singlePieceSale.total, 0.001)
    assertEquals(24.0, singlePieceSale.unitsPerPackage, 0.001)
  }

  @Test
  fun `json converter persistence for both package and single piece items`() {
    val converters = com.example.data.Converters()

    val packageItem = InvoiceItem(
      name = "كرتونة عصير 24 حبة",
      unitsPerPackage = 24.0,
      packagesCount = 2.0,
      saleUnit = SaleUnit.PACKAGE,
      quantity = 48.0,
      unitPrice = 2.5
    )

    val pieceItem = InvoiceItem(
      name = "حبة مياه مفردة",
      unitsPerPackage = 40.0,
      packagesCount = 0.0,
      singlePiecesCount = 7.0,
      saleUnit = SaleUnit.PIECE,
      quantity = 7.0,
      unitPrice = 1.0
    )

    val jsonString = converters.fromInvoiceItemsList(listOf(packageItem, pieceItem))
    assertTrue(jsonString.contains("PACKAGE"))
    assertTrue(jsonString.contains("PIECE"))

    val restored = converters.toInvoiceItemsList(jsonString)
    assertEquals(2, restored.size)

    assertEquals(SaleUnit.PACKAGE, restored[0].saleUnit)
    assertEquals(24.0, restored[0].unitsPerPackage, 0.001)
    assertEquals(2.0, restored[0].packagesCount, 0.001)
    assertEquals(48.0, restored[0].quantity, 0.001)
    assertEquals(120.0, restored[0].total, 0.001)

    assertEquals(SaleUnit.PIECE, restored[1].saleUnit)
    assertEquals(40.0, restored[1].unitsPerPackage, 0.001)
    assertEquals(7.0, restored[1].quantity, 0.001)
    assertEquals(7.0, restored[1].total, 0.001)
  }
}
