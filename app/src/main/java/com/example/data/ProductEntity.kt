package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val defaultUnitPrice: Double,
    val unitsPerPackage: Double,
    val packageTypeName: String,
    val description: String
) {
    fun toDomain(): Product = Product(
        id = id,
        name = name,
        defaultUnitPrice = defaultUnitPrice,
        unitsPerPackage = unitsPerPackage,
        packageTypeName = packageTypeName,
        description = description
    )

    companion object {
        fun fromDomain(product: Product): ProductEntity = ProductEntity(
            id = product.id,
            name = product.name,
            defaultUnitPrice = product.defaultUnitPrice,
            unitsPerPackage = product.unitsPerPackage,
            packageTypeName = product.packageTypeName,
            description = product.description
        )
    }
}
