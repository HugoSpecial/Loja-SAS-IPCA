package ipca.project.lojasas.models

import java.util.Date

data class StockItem(
    var docId: String = "",
    var name: String = "",
    var imageUrl: String = "",
    var batches: MutableList<ProductBatch> = mutableListOf()
) {
    fun getTotalQuantity(): Int {
        return batches.sumOf { it.quantity }
    }
}