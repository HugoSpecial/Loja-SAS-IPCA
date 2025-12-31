package ipca.project.lojasas.ui.collaborator.delivery

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.project.lojasas.models.Delivery
import ipca.project.lojasas.models.DeliveryState
import ipca.project.lojasas.models.Order
import ipca.project.lojasas.models.OrderItem
import ipca.project.lojasas.models.OrderState
import ipca.project.lojasas.models.Product
import ipca.project.lojasas.models.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

// --- MODELOS AUXILIARES ---
data class CartItem(
    val product: Product,
    val quantity: Int
)

// --- ESTADO DA UI ---
data class UrgentDeliveryState(
    val products: List<Product> = emptyList(),
    val cart: List<CartItem> = emptyList(),

    // Dados Beneficiário
    val beneficiaryName: String = "",
    val selectedUser: User? = null,
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,

    // Estados Gerais
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class UrgentDeliveryViewModel : ViewModel() {

    var uiState = mutableStateOf(UrgentDeliveryState())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private var searchJob: Job? = null

    init {
        fetchProducts()
    }

    // 1. CARREGAR PRODUTOS (STOCK)
    private fun fetchProducts() {
        uiState.value = uiState.value.copy(isLoading = true)
        db.collection("products").whereGreaterThan("batches", emptyList<Any>())
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { doc ->
                    val p = doc.toObject(Product::class.java)
                    p?.docId = doc.id
                    p
                }
                uiState.value = uiState.value.copy(products = list, isLoading = false)
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao carregar stock.")
            }
    }

    // 2. PESQUISA INTELIGENTE (AUTOCOMPLETE)
    fun onBeneficiaryNameChange(inputText: String) {
        uiState.value = uiState.value.copy(
            beneficiaryName = inputText,
            selectedUser = null
        )
        searchJob?.cancel()

        if (inputText.isBlank()) {
            uiState.value = uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce de 300ms
            performSmartSearch(inputText)
        }
    }

    private fun performSmartSearch(query: String) {
        uiState.value = uiState.value.copy(isSearching = true)

        // Query "range" para encontrar prefixos (ex: "J" encontra "Joao", "Jose")
        db.collection("users")
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                val users = result.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)
                    user?.docId = doc.id
                    user
                }
                uiState.value = uiState.value.copy(searchResults = users, isSearching = false)
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(searchResults = emptyList(), isSearching = false)
            }
    }

    fun onUserSelected(user: User) {
        uiState.value = uiState.value.copy(
            beneficiaryName = user.name ?: "",
            selectedUser = user,
            searchResults = emptyList()
        )
    }

    // 3. GESTÃO DO CARRINHO
    fun addToCart(product: Product, quantity: Int) {
        val currentCart = uiState.value.cart.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.docId == product.docId }

        if (existingIndex != -1) {
            val existingItem = currentCart[existingIndex]
            val newQuantity = existingItem.quantity + quantity

            if (newQuantity <= 0) {
                currentCart.removeAt(existingIndex)
            } else {
                currentCart[existingIndex] = existingItem.copy(quantity = newQuantity)
            }
        } else {
            if (quantity > 0) {
                currentCart.add(CartItem(product, quantity))
            }
        }
        uiState.value = uiState.value.copy(cart = currentCart)
    }

    fun removeFromCart(item: CartItem) {
        val currentCart = uiState.value.cart.toMutableList()
        currentCart.remove(item)
        uiState.value = uiState.value.copy(cart = currentCart)
    }

    // 4. SUBMISSÃO DA ENTREGA
    fun submitUrgentDelivery(onSuccess: () -> Unit) {
        val state = uiState.value

        if (state.cart.isEmpty()) {
            uiState.value = state.copy(error = "Carrinho vazio.")
            return
        }
        if (state.beneficiaryName.isBlank()) {
            uiState.value = state.copy(error = "Indique o beneficiário.")
            return
        }

        uiState.value = state.copy(isLoading = true, error = null)

        val orderItems = state.cart.map { OrderItem(name = it.product.name, quantity = it.quantity) }.toMutableList()
        val userId = state.selectedUser?.docId
        val userName = state.beneficiaryName

        val newOrder = Order(
            orderDate = Date(),
            surveyDate = Date(),
            accept = OrderState.ACEITE,
            items = orderItems,
            userId = userId,
            userName = userName,
            evaluatedBy = auth.currentUser?.uid
        )

        db.collection("orders").add(newOrder).addOnSuccessListener { orderRef ->
            val newDelivery = Delivery(
                orderId = orderRef.id,
                delivered = true,
                state = DeliveryState.ENTREGUE,
                reason = "Entrega Urgente",
                evaluatedBy = auth.currentUser?.uid,
                evaluationDate = Date(),
                surveyDate = Date()
            )

            db.collection("delivery").add(newDelivery).addOnSuccessListener {
                updateStock(state.cart) {
                    uiState.value = state.copy(isLoading = false, success = true)
                    onSuccess()
                }
            }.addOnFailureListener { e -> uiState.value = state.copy(isLoading = false, error = e.message) }
        }.addOnFailureListener { e -> uiState.value = state.copy(isLoading = false, error = e.message) }
    }

    private fun updateStock(cartItems: List<CartItem>, onComplete: () -> Unit) {
        var processed = 0
        if (cartItems.isEmpty()) { onComplete(); return }

        cartItems.forEach { item ->
            val productRef = db.collection("products").document(item.product.docId!!)
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
                if (processed == cartItems.size) onComplete()
            }
        }
    }
}