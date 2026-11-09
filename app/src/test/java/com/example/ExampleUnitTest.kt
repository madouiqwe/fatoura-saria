package com.example

import com.madoui.fatoura.data.Invoice
import com.madoui.fatoura.data.InvoiceItem
import com.madoui.fatoura.data.SaleUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testPiecesCalculationFromReceipt() {
    val item1 = InvoiceItem(
      id = 1L,
      name = "350 × 12",
      unit = SaleUnit.PIECE,
      quantity = 12.0,
      unitsPerPackage = 1,
      unitPrice = 350.0
    )

    val item2 = InvoiceItem(
      id = 2L,
      name = "420 × 12",
      unit = SaleUnit.PIECE,
      quantity = 12.0,
      unitsPerPackage = 1,
      unitPrice = 420.0
    )

    assertEquals(12.0, item1.totalPieces, 0.001)
    assertEquals(350.0, item1.piecePrice, 0.001)
    assertEquals(4200.0, item1.lineTotal, 0.001)

    assertEquals(12.0, item2.totalPieces, 0.001)
    assertEquals(420.0, item2.piecePrice, 0.001)
    assertEquals(5040.0, item2.lineTotal, 0.001)

    val invoice = Invoice(
      number = "868056",
      items = listOf(item1, item2)
    )

    assertEquals(24.0, invoice.totalPieces, 0.001)
    assertEquals(9240.0, invoice.total, 0.001)
  }

  @Test
  fun testPackagePiecesCalculation() {
    val packageItem = InvoiceItem(
      id = 3L,
      name = "منتج حزمة",
      unit = SaleUnit.PACKAGE,
      quantity = 1.0,
      unitsPerPackage = 12,
      unitPrice = 4200.0
    )

    assertEquals(12.0, packageItem.totalPieces, 0.001)
    assertEquals(350.0, packageItem.piecePrice, 0.001)
    assertEquals(4200.0, packageItem.lineTotal, 0.001)
  }
}
