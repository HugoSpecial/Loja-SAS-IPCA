package ipca.project.lojasas.ui.collaborator.delivery

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.Product
import java.util.Date

data class CartItem(
    val product: Product,
    val quantity: Int
)

data class UrgentDeliveryState(
    val products: List<Product> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val beneficiaryName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class UrgentDeliveryViewModel : ViewModel() {

    var uiState = mutableStateOf(UrgentDeliveryState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    init {
        fetchProducts()
    }

    private fun fetchProducts() {
        uiState.value = uiState.value.copy(isLoading = true)
        db.collection("products").whereGreaterThan("batches", emptyList<Any>())
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Product::class.java)
                list.forEachIndexed { i, p -> p.docId = result.documents[i].id }
                uiState.value = uiState.value.copy(products = list, isLoading = false)
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao carregar stock.")
            }
    }

    fun onBeneficiaryNameChange(name: String) {
        uiState.value = uiState.value.copy(beneficiaryName = name)
    }

    fun addToCart(product: Product, quantity: Int) {
        if (quantity <= 0) return

        val currentCart = uiState.value.cart.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.docId == product.docId }

        if (existingIndex != -1) {
            val existingItem = currentCart[existingIndex]
            currentCart[existingIndex] = existingItem.copy(quantity = existingItem.quantity + quantity)
        } else {
            currentCart.add(CartItem(product, quantity))
        }

        uiState.value = uiState.value.copy(cart = currentCart)
    }

    fun removeFromCart(item: CartItem) {
        val currentCart = uiState.value.cart.toMutableList()
        currentCart.remove(item)
        uiState.value = uiState.value.copy(cart = currentCart)
    }

    fun submitUrgentDelivery(onSuccess: () -> Unit) {
        val state = uiState.value

        if (state.cart.isEmpty()) {
            uiState.value = state.copy(error = "O carrinho está vazio.")
            return
        }
        if (state.beneficiaryName.isBlank()) {
            uiState.value = state.copy(error = "Indique o nome do beneficiário (ou 'Anónimo').")
            return
        }

        uiState.value = state.copy(isLoading = true, error = null)

        val orderItems = state.cart.map {
            OrderItem(name = it.product.name, quantity = it.quantity)
        }.toMutableList()

        val newOrder = Order(
            orderDate = Date(),
            surveyDate = Date(),
            accept = OrderState.ACEITE,
            items = orderItems,
            userId = null,
            userName = state.beneficiaryName,
            evaluatedBy = auth.currentUser?.uid
        )

        db.collection("orders").add(newOrder)
            .addOnSuccessListener { orderRef ->
                val orderId = orderRef.id

                val deliveryData = hashMapOf(
                    "orderId" to orderId,
                    "delivered" to true,
                    "state" to "ENTREGUE",
                    "reason" to "Entrega Urgente",
                    "evaluatedBy" to auth.currentUser?.uid,
                    "evaluationDate" to Date(),
                    "surveyDate" to Date()
                )

                db.collection("delivery").add(deliveryData)
                    .addOnSuccessListener {
                        updateStock(state.cart) {
                            uiState.value = state.copy(isLoading = false, success = true)
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        uiState.value = state.copy(isLoading = false, error = "Falha ao criar entrega: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                uiState.value = state.copy(isLoading = false, error = "Falha ao criar pedido: ${e.message}")
            }
    }

    private fun updateStock(cartItems: List<CartItem>, onComplete: () -> Unit) {
        var processed = 0
        if (cartItems.isEmpty()) {
            onComplete()
            return
        }

        cartItems.forEach { item ->
            val productRef = db.collection("products").document(item.product.docId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(productRef)
                val product = snapshot.toObject(Product::class.java)

                if (product != null) {
                    var qtyToDeduct = item.quantity
                    val batches = product.batches.sortedBy { it.validity }.toMutableList()
                    val iterator = batches.listIterator()

                    while (iterator.hasNext() && qtyToDeduct > 0) {
                        val batch = iterator.next()
                        if (batch.quantity > qtyToDeduct) {
                            batch.quantity -= qtyToDeduct
                            qtyToDeduct = 0
                        } else {
                            qtyToDeduct -= batch.quantity
                            iterator.remove()
                        }
                    }
                    transaction.update(productRef, "batches", batches)
                }
            }.addOnCompleteListener {
                processed++
                if (processed == cartItems.size) {
                    onComplete()
                }
            }
        }
    }
}