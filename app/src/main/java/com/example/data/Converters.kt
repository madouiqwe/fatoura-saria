package com.example.data

import androidx.room.TypeConverter
import com.example.model.InvoiceItem
import com.example.model.SaleUnit
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromInvoiceItemsList(items: List<InvoiceItem>?): String {
        if (items.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("packageInfo", item.packageInfo)
                put("unitsPerPackage", item.unitsPerPackage)
                put("packagesCount", item.packagesCount)
                put("singlePiecesCount", item.singlePiecesCount)
                put("saleUnit", item.saleUnit.name)
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
                put("description", item.description)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toInvoiceItemsList(data: String?): List<InvoiceItem> {
        if (data.isNullOrBlank()) return emptyList()
        val list = mutableListOf<InvoiceItem>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawQty = obj.optDouble("quantity", 1.0)
                val units = obj.optDouble("unitsPerPackage", 12.0)
                val packs = obj.optDouble("packagesCount", if (units > 1.0) (rawQty / units) else 1.0)
                val singlePieces = obj.optDouble("singlePiecesCount", rawQty)

                val saleUnitStr = obj.optString("saleUnit", "")
                val saleUnit = try {
                    if (saleUnitStr.isNotBlank()) {
                        SaleUnit.valueOf(saleUnitStr)
                    } else {
                        // Fallback inference for old data
                        if (units > 1.0 && packs > 0) SaleUnit.PACKAGE else SaleUnit.PIECE
                    }
                } catch (_: Exception) {
                    SaleUnit.PACKAGE
                }

                val finalQty = if (saleUnit == SaleUnit.PACKAGE) {
                    (units * packs).coerceAtLeast(0.0)
                } else {
                    singlePieces.coerceAtLeast(0.0)
                }

                val item = InvoiceItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    packageInfo = obj.optString("packageInfo", ""),
                    unitsPerPackage = units,
                    packagesCount = packs,
                    singlePiecesCount = singlePieces,
                    saleUnit = saleUnit,
                    quantity = finalQty,
                    unitPrice = obj.optDouble("unitPrice", 0.0),
                    description = obj.optString("description", "")
                )
                list.add(item)
            }
        } catch (_: Exception) {
            // Return empty list on parse failure
        }
        return list
    }
}
