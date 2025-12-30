package ipca.project.lojasas.ui.beneficiary

import androidx.compose.runtime.mutableStateMapOf

object CartManager {
    // Guarda: ID do Produto -> Quantidade (por defeito começa a 1)
    val cartItems = mutableStateMapOf<String, Int>()

    // Usado na Home (Adicionar/Remover)
    fun toggleProduct(productId: String) {
        if (cartItems.containsKey(productId)) {
            cartItems.remove(productId)
        } else {
            cartItems[productId] = 1
        }
    }

    // Usado no Cesto (Aumentar quantidade)
    fun addQuantity(productId: String) {
        val current = cartItems[productId] ?: 0
        cartItems[productId] = current + 1
    }

    // Usado no Cesto (Diminuir quantidade)
    fun removeQuantity(productId: String) {
        val current = cartItems[productId] ?: 0
        if (current > 1) {
            cartItems[productId] = current - 1
        } else {
            cartItems.remove(productId)
        }
    }

    // Limpar tudo após encomendar
    fun clear() {
        cartItems.clear()
    }
}