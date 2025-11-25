package ipca.project.lojasas.models

import java.util.Date

data class ProductBatch(
    var validity: Date? = null,
    var quantity: Int = 0
)

data class StockItem(
    var docId: String = "",
    var name: String = "",
    var imageUrl: String = "",
    // Lista de lotes
    var batches: MutableList<ProductBatch> = mutableListOf()
) {
    fun getTotalQuantity(): Int {
        return batches.sumOf { it.quantity }
    }
}