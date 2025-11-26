package ipca.project.lojasas.models

import java.util.Date

data class ProductBatch(
    var validity: Date = Date(),
    var quantity: Int = 0
)

data class Product (
    var docId : String = "",
    var name : String = "",
    var imageUrl : String = "",
    var batches : MutableList<ProductBatch> = mutableListOf()
)