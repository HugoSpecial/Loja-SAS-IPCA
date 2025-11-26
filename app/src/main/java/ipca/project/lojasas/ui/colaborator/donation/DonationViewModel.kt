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
import com.google.firebase.firestore.FieldValue // <--- IMPORTANTE
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Campaign
import ipca.project.lojasas.models.Donation
import ipca.project.lojasas.models.Product
import ipca.project.lojasas.models.ProductBatch
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Date

data class DonationState(
    var currentName: String = "",
    var currentQuantity: String = "",
    var currentValidity: Date? = null,
    var currentImageBase64: String? = null,
    var productsToAdd: MutableList<Product> = mutableListOf(),
    var isAnonymous: Boolean = false,
    var donorName: String = "",
    var selectedCampaign: Campaign? = null,
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

    fun loadProduct(productId: String?) {
        if (productId.isNullOrEmpty()) return
        uiState.value = uiState.value.copy(isLoading = true)
        db.collection("products").document(productId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val item = doc.toObject(Product::class.java)
                uiState.value = uiState.value.copy(
                    currentName = item?.name ?: "",
                    currentImageBase64 = item?.imageUrl,
                    isLoading = false
                )
            } else {
                uiState.value = uiState.value.copy(isLoading = false)
            }
        }
    }

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

    fun addProductToList() {
        val state = uiState.value
        if (state.currentName.isBlank() || state.currentQuantity.isBlank()) {
            uiState.value = uiState.value.copy(error = "Preencha nome e quantidade.")
            return
        }
        val qty = state.currentQuantity.toInt()
        val validDate = state.currentValidity ?: Date()
        val newProduct = Product(
            name = state.currentName,
            imageUrl = state.currentImageBase64 ?: "",
            batches = mutableListOf(ProductBatch(validity = validDate, quantity = qty))
        )
        val currentList = state.productsToAdd.toMutableList()
        currentList.add(newProduct)
        uiState.value = uiState.value.copy(
            productsToAdd = currentList,
            currentName = "", currentQuantity = "", currentValidity = null, currentImageBase64 = null, error = null
        )
    }

    fun removeProductFromList(index: Int) {
        val currentList = uiState.value.productsToAdd.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            uiState.value = uiState.value.copy(productsToAdd = currentList)
        }
    }

    fun onCampaignSelected(campaign: Campaign) { uiState.value = uiState.value.copy(selectedCampaign = campaign) }
    fun onAnonymousChange(isAnonymous: Boolean) { uiState.value = uiState.value.copy(isAnonymous = isAnonymous, donorName = if(isAnonymous) "" else uiState.value.donorName) }
    fun onDonorNameChange(text: String) { uiState.value = uiState.value.copy(donorName = text) }

    // --- LÓGICA DE GUARDAR (Atualizada) ---
    fun saveDonation(onSuccess: () -> Unit) {
        val state = uiState.value

        if (state.productsToAdd.isEmpty()) {
            uiState.value = uiState.value.copy(error = "Adicione produtos à lista.")
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

        // 1. Criar a Doação
        db.collection("donations").add(donation)
            .addOnSuccessListener { docRef ->
                val newDonationId = docRef.id

                // 2. SE TIVER CAMPANHA, ATUALIZAR A LISTA DA CAMPANHA NA BD
                val campaignId = state.selectedCampaign?.docId
                if (!campaignId.isNullOrEmpty()) {
                    db.collection("campaigns").document(campaignId)
                        .update("donations", FieldValue.arrayUnion(newDonationId))
                        .addOnFailureListener { e ->
                            // Apenas logar o erro, não impede o sucesso da doação
                            println("Erro ao atualizar campanha: ${e.message}")
                        }
                }

                // 3. Atualizar Stock de Produtos
                updateStockForList(state.productsToAdd, onSuccess)
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${it.message}")
            }
    }

    private fun updateStockForList(products: List<Product>, onComplete: () -> Unit) {
        var processedCount = 0
        if (products.isEmpty()) { onComplete(); return }

        for (product in products) {
            val qty = product.batches[0].quantity
            val date = product.batches[0].validity
            val img = product.imageUrl
            val name = product.name

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
                    if (!img.isNullOrEmpty()) updates["imageUrl"] = img
                    doc.reference.update(updates).addOnCompleteListener { onDone() }
                } else {
                    val newBatch = ProductBatch(validity = date, quantity = qty)
                    val newItem = Product(name = name, imageUrl = img ?: "", batches = mutableListOf(newBatch))
                    db.collection("products").add(newItem).addOnCompleteListener { onDone() }
                }
            }
    }

    // --- Helpers ---
    private fun isSameDay(date1: Date?, date2: Date?): Boolean {
        if (date1 == null && date2 == null) return true
        if (date1 == null || date2 == null) return false
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width; val originalHeight = bitmap.height
        var resizedWidth = maxDimension; var resizedHeight = maxDimension
        if (originalHeight > originalWidth) { resizedHeight = maxDimension; resizedWidth = (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt() }
        else if (originalWidth > originalHeight) { resizedWidth = maxDimension; resizedHeight = (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt() }
        else { if (originalHeight < maxDimension) return bitmap }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}