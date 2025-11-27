package ipca.project.lojasas.ui.benefeciary.newBasket

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
import java.util.*

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

    // Inicializa e carrega produtos
    init{
        uiState.value = uiState.value.copy(isLoading = true)
        fetchProducts()
    }

    private fun fetchProducts() {
        db.collection("products")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NewBasketVM", "Erro ao carregar produtos", error)
                    uiState.value = uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val productList = mutableListOf<Product>()
                for (doc in snapshot?.documents ?: emptyList()) {
                    try {
                        val product = doc.toObject(Product::class.java)
                        product?.docId = doc.id
                        if (product != null) productList.add(product)
                    } catch (e: Exception) {
                        Log.e("NewBasketVM", "Erro ao converter produto", e)
                    }
                }

                uiState.value = uiState.value.copy(products = productList, isLoading = false, error = null)
            }
    }

    // Cria um novo pedido com os produtos selecionados
    fun createOrder(
        selectedDate: Date,
        selectedProducts: Map<String, Int>,
        onSubmitResult: (Boolean) -> Unit
    ) {
        val orderProducts = selectedProducts.filter { it.value > 0 }
        if (orderProducts.isEmpty()) {
            uiState.value = uiState.value.copy(error = "Selecione pelo menos um produto")
            return
        }

        val newOrder = Order(
            docId = null,
            orderDate = Date(),
            surveyDate = selectedDate,
            accept = OrderState.PENDENTE,
            items = orderProducts.map { (productId, quantity) ->
                OrderItem(name = uiState.value.products.find { it.docId == productId }?.name, quantity = quantity)
            }.toMutableList(),
            userId = auth.currentUser?.uid
        )

        db.collection("orders")
            .add(newOrder)
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(orderCreated = true)
                onSubmitResult(true)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(error = e.message)
                onSubmitResult(false)
            }
    }
}
