package ipca.project.lojasas.ui.colaborator.donation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Campaign
import ipca.project.lojasas.models.Donation
import ipca.project.lojasas.models.Product
import ipca.project.lojasas.models.ProductBatch
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Date

data class DonationState(
    // Campos do Produto Atual (Input)
    var currentName: String = "",
    var currentQuantity: String = "",
    var currentValidity: Date? = null,
    var currentImageBase64: String? = null,

    // Lista de Produtos já adicionados à doação
    var productsToAdd: MutableList<Product> = mutableListOf(),

    // Dados da Doação (Geral)
    var isAnonymous: Boolean = false,
    var donorName: String = "",
    var selectedCampaign: Campaign? = null,

    // Listas Auxiliares
    var activeCampaigns: List<Campaign> = emptyList(),
    var existingProducts: List<Product> = emptyList(),
    var filteredProducts: List<Product> = emptyList(),

    var isLoading: Boolean = false,
    var error: String? = null,
    var isSuccess: Boolean = false
)

class DonationViewModel : ViewModel() {

    var uiState = mutableStateOf(DonationState())
        private set

    private val db = Firebase.firestore

    init {
        fetchActiveCampaigns()
        fetchAllProducts()
    }

    private fun fetchActiveCampaigns() {
        val today = Date()
        db.collection("campaigns").get().addOnSuccessListener { result ->
            val campaigns = result.toObjects(Campaign::class.java).filter {
                val start = it.startDate ?: Date(0)
                val end = it.endDate ?: Date(Long.MAX_VALUE)
                today.after(start) && today.before(end)
            }
            campaigns.forEachIndexed { index, c -> c.docId = result.documents[index].id }
            uiState.value = uiState.value.copy(activeCampaigns = campaigns)
        }
    }

    private fun fetchAllProducts() {
        db.collection("products").get().addOnSuccessListener { result ->
            val products = result.toObjects(Product::class.java)
            uiState.value = uiState.value.copy(existingProducts = products)
        }
    }

    // --- INPUTS PRODUTO ATUAL ---
    fun onNameChange(text: String) {
        val filtered = if (text.isBlank()) emptyList() else {
            uiState.value.existingProducts.filter { it.name.contains(text, ignoreCase = true) }
        }
        uiState.value = uiState.value.copy(currentName = text, filteredProducts = filtered)
    }

    fun onProductSelected(product: Product) {
        uiState.value = uiState.value.copy(
            currentName = product.name,
            currentImageBase64 = product.imageUrl,
            filteredProducts = emptyList()
        )
    }

    fun onQuantityChange(text: String) { if (text.all { it.isDigit() }) uiState.value = uiState.value.copy(currentQuantity = text) }
    fun onDateSelected(date: Date) { uiState.value = uiState.value.copy(currentValidity = date) }

    fun onImageSelected(context: Context, uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            val scaledBitmap = scaleBitmapDown(bitmap, 800)
            val base64 = bitmapToBase64(scaledBitmap)
            uiState.value = uiState.value.copy(currentImageBase64 = base64)
        } catch (e: Exception) { }
    }

    // --- ADICIONAR PRODUTO À LISTA TEMPORÁRIA ---
    fun addProductToList() {
        val state = uiState.value
        if (state.currentName.isBlank() || state.currentQuantity.isBlank()) {
            uiState.value = uiState.value.copy(error = "Preencha nome e quantidade do produto.")
            return
        }

        val qty = state.currentQuantity.toInt()
        val validDate = state.currentValidity ?: Date()

        val newProduct = Product(
            name = state.currentName,
            imageUrl = state.currentImageBase64 ?: "",
            batches = mutableListOf(ProductBatch(validity = validDate, quantity = qty))
        )

        // Adiciona à lista e limpa os campos
        val currentList = state.productsToAdd.toMutableList()
        currentList.add(newProduct)

        uiState.value = uiState.value.copy(
            productsToAdd = currentList,
            currentName = "",
            currentQuantity = "",
            currentValidity = null,
            currentImageBase64 = null,
            error = null
        )
    }

    fun removeProductFromList(index: Int) {
        val currentList = uiState.value.productsToAdd.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            uiState.value = uiState.value.copy(productsToAdd = currentList)
        }
    }

    // --- INPUTS GERAIS ---
    fun onCampaignSelected(campaign: Campaign) { uiState.value = uiState.value.copy(selectedCampaign = campaign) }
    fun onAnonymousChange(isAnonymous: Boolean) { uiState.value = uiState.value.copy(isAnonymous = isAnonymous, donorName = if(isAnonymous) "" else uiState.value.donorName) }
    fun onDonorNameChange(text: String) { uiState.value = uiState.value.copy(donorName = text) }

    // --- CARREGAR DADOS SE VIER DO STOCK (Opcional) ---
    fun loadProduct(productId: String?) {
        if (productId.isNullOrEmpty()) return
        db.collection("products").document(productId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val item = doc.toObject(Product::class.java)
                uiState.value = uiState.value.copy(
                    currentName = item?.name ?: "",
                    currentImageBase64 = item?.imageUrl
                )
            }
        }
    }

    // --- GUARDAR TUDO ---
    fun saveDonation(onSuccess: () -> Unit) {
        val state = uiState.value

        if (state.productsToAdd.isEmpty()) {
            uiState.value = uiState.value.copy(error = "Adicione pelo menos um produto à doação.")
            return
        }
        if (!state.isAnonymous && state.donorName.isBlank()) {
            uiState.value = uiState.value.copy(error = "Indique o doador.")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true, error = null)

        val totalQty = state.productsToAdd.sumOf { p -> p.batches.sumOf { b -> b.quantity } }

        val donation = Donation(
            name = if (state.isAnonymous) "Anónimo" else state.donorName,
            quantity = totalQty,
            donationDate = Date(),
            anonymous = state.isAnonymous,
            products = state.productsToAdd,
            campaignId = state.selectedCampaign?.docId
        )

        db.collection("donations").add(donation)
            .addOnSuccessListener {
                // Atualizar Stock de CADA produto na lista
                updateStockForList(state.productsToAdd, onSuccess)
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${it.message}")
            }
    }

    // Função recursiva ou iterativa para atualizar vários produtos
    private fun updateStockForList(products: List<Product>, onComplete: () -> Unit) {
        var processedCount = 0

        if (products.isEmpty()) {
            onComplete()
            return
        }

        for (product in products) {
            val qty = product.batches[0].quantity
            val date = product.batches[0].validity
            val img = product.imageUrl
            val name = product.name

            // Chama a função de update individual
            updateSingleProductStock(name, qty, date, img) {
                processedCount++
                if (processedCount == products.size) {
                    uiState.value = uiState.value.copy(isLoading = false, isSuccess = true)
                    onComplete()
                }
            }
        }
    }

    private fun updateSingleProductStock(name: String, qty: Int, date: Date, img: String?, onDone: () -> Unit) {
        db.collection("products").whereEqualTo("name", name.trim()).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.documents[0]
                    val item = doc.toObject(Product::class.java)!!
                    val batches = item.batches
                    var found = false
                    for (b in batches) {
                        if (isSameDay(b.validity, date)) {
                            b.quantity += qty
                            found = true; break
                        }
                    }
                    if (!found) batches.add(ProductBatch(validity = date, quantity = qty))

                    val updates = mutableMapOf<String, Any>("batches" to batches)
                    if (!img.isNullOrEmpty()) updates["imageUrl"] = img // Atualiza foto se houver nova

                    doc.reference.update(updates).addOnCompleteListener { onDone() }
                } else {
                    val newBatch = ProductBatch(validity = date, quantity = qty)
                    val newItem = Product(name = name, imageUrl = img ?: "", batches = mutableListOf(newBatch))
                    db.collection("products").add(newItem).addOnCompleteListener { onDone() }
                }
            }
    }

    // (Funções utilitárias isSameDay, scale, base64 mantêm-se iguais)
    private fun isSameDay(date1: Date?, date2: Date?): Boolean {
        if (date1 == null && date2 == null) return true
        if (date1 == null || date2 == null) return false
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap { return bitmap } // Simplificado aqui
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}