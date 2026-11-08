package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.PaymentMethod

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val issueDate: Long,
    val dueDate: Long,
    val status: String,
    val paymentMethod: String,
    val items: List<InvoiceItem>,
    val discountAmount: Double,
    val isDiscountPercentage: Boolean,
    val taxPercentage: Double,
    val paidAmount: Double,
    val notes: String,
    val terms: String,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val isEditedAfterCompletion: Boolean
) {
    fun toDomain(): Invoice {
        val invoiceStatus = try {
            InvoiceStatus.valueOf(status)
        } catch (_: Exception) {
            InvoiceStatus.PENDING
        }
        val pMethod = try {
            PaymentMethod.valueOf(paymentMethod)
        } catch (_: Exception) {
            PaymentMethod.CASH
        }

        return Invoice(
            id = id,
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            issueDate = issueDate,
            dueDate = dueDate,
            status = invoiceStatus,
            paymentMethod = pMethod,
            items = items,
            discountAmount = discountAmount,
            isDiscountPercentage = isDiscountPercentage,
            taxPercentage = taxPercentage,
            paidAmount = paidAmount,
            notes = notes,
            terms = terms,
            createdAt = createdAt,
            updatedAt = updatedAt,
            completedAt = completedAt,
            isEditedAfterCompletion = isEditedAfterCompletion
        )
    }

    companion object {
        fun fromDomain(invoice: Invoice): InvoiceEntity {
            return InvoiceEntity(
                id = invoice.id,
                invoiceNumber = invoice.invoiceNumber,
                customerName = invoice.customerName,
                customerPhone = invoice.customerPhone,
                customerEmail = invoice.customerEmail,
                issueDate = invoice.issueDate,
                dueDate = invoice.dueDate,
                status = invoice.status.name,
                paymentMethod = invoice.paymentMethod.name,
                items = invoice.items,
                discountAmount = invoice.discountAmount,
                isDiscountPercentage = invoice.isDiscountPercentage,
                taxPercentage = invoice.taxPercentage,
                paidAmount = invoice.paidAmount,
                notes = invoice.notes,
                terms = invoice.terms,
                createdAt = invoice.createdAt,
                updatedAt = invoice.updatedAt,
                completedAt = invoice.completedAt,
                isEditedAfterCompletion = invoice.isEditedAfterCompletion
            )
        }
    }
}
