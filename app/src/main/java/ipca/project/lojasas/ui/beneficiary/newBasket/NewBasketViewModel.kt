package ipca.project.lojasas.ui.beneficiary.newBasket

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.Product
import java.util.Date

data class NewBasketState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val orderCreated: Boolean = false
)

class NewBasketViewModel : ViewModel() {
    var uiState = mutableStateOf(NewBasketState())
        private set
    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("products").orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val productList = mutableListOf<Product>()
                val today = Date()

                for (doc in snapshot?.documents ?: emptyList()) {
                    try {
                        val product = doc.toObject(Product::class.java)
                        product?.docId = doc.id

                        if (product != null) {
                            val validBatchesForDisplay = product.batches.filter { batch ->
                                val isValidDate = batch.validity?.after(today) == true || isSameDay(batch.validity, today)
                                isValidDate && batch.quantity > 0
                            }.toMutableList()

                            // Substituímos os lotes do objeto local apenas para cálculo de UI
                            product.batches = validBatchesForDisplay

                            // Só mostramos o produto se tiver stock VÁLIDO > 0
                            if (product.batches.isNotEmpty()) {
                                productList.add(product)
                            }
                        }
                    } catch (e: Exception) { Log.e("NewBasketVM", "Erro", e) }
                }
                uiState.value = uiState.value.copy(products = productList, isLoading = false, error = null)
            }
    }

    private fun isSameDay(date1: Date?, date2: Date?): Boolean {
        if (date1 == null || date2 == null) return false
        val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    fun createOrder(selectedDate: Date, selectedProducts: Map<String, Int>, onSubmitResult: (Boolean) -> Unit) {
        val orderProducts = selectedProducts.filter { it.value > 0 }
        if (orderProducts.isEmpty()) {
            uiState.value = uiState.value.copy(error = "Selecione pelo menos um produto")
            return
        }
        val userId = auth.currentUser?.uid
        if (userId == null) {
            uiState.value = uiState.value.copy(error = "Utilizador não autenticado")
            onSubmitResult(false)
            return
        }

        db.collection("users").document(userId).get().addOnSuccessListener { snapshot ->
            val userName = snapshot.getString("name") ?: "Sem nome"
            val newOrder = Order(
                docId = null,
                orderDate = Date(),
                surveyDate = selectedDate,
                accept = OrderState.PENDENTE,
                userId = userId,
                userName = userName,
                items = orderProducts.map { (productId, quantity) ->
                    OrderItem(
                        name = uiState.value.products.find { it.docId == productId }?.name ?: "Produto",
                        quantity = quantity
                    )
                }.toMutableList()
            )

            db.collection("orders").add(newOrder).addOnSuccessListener {
                uiState.value = uiState.value.copy(orderCreated = true)
                onSubmitResult(true)
            }.addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
                onSubmitResult(false)
            }
        }
    }
}