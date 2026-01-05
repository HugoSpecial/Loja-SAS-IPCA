package ipca.project.lojasas.ui.collaborator.donation

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
import com.google.firebase.firestore.FieldValue
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
    var currentCategory: String = "",
    var currentQuantity: String = "",
    var currentValidity: Date? = null,
    var currentImageBase64: String? = null,

    // Lista de categorias disponíveis
    val categories: List<String> = listOf("Alimentos", "Higiene", "Limpeza"),

    var productsToAdd: MutableList<Product> = mutableListOf(),
    var isAnonymous: Boolean = false,
    var donorName: String = "",
    var selectedCampaign: Campaign? = null,
    var activeCampaigns: List<Campaign> = emptyList(),
    var existingProducts: List<Product> = emptyList(), // Lista local para validação
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
        // Carrega todos os produtos para memória para validação rápida
        db.collection("products").get().addOnSuccessListener { result ->
            val products = mutableListOf<Product>()
            for (doc in result) {
                val p = doc.toObject(Product::class.java)
                p.docId = doc.id
                products.add(p)
            }
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
                    currentCategory = item?.category ?: "",
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

    fun onCategoryChange(category: String) {
        uiState.value = uiState.value.copy(currentCategory = category)
    }

    fun onProductSelected(product: Product) {
        uiState.value = uiState.value.copy(
            currentName = product.name,
            currentCategory = product.category,
            currentImageBase64 = product.imageUrl,
            filteredProducts = emptyList()
        )
    }

    fun onQuantityChange(text: String) {
        if (text.all { it.isDigit() }) uiState.value = uiState.value.copy(currentQuantity = text)
    }

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
        if (state.currentCategory.isBlank()) {
            uiState.value = uiState.value.copy(error = "Selecione a categoria do produto.")
            return
        }

        val qty = state.currentQuantity.toInt()
        val validDate = state.currentValidity ?: Date()

        val newProduct = Product(
            name = state.currentName.trim(), // Importante: Trim para evitar espaços extra
            category = state.currentCategory,
            imageUrl = state.currentImageBase64 ?: "",
            batches = mutableListOf(ProductBatch(validity = validDate, quantity = qty))
        )
        val currentList = state.productsToAdd.toMutableList()
        currentList.add(newProduct)

        uiState.value = uiState.value.copy(
            productsToAdd = currentList,
            currentName = "",
            currentCategory = "",
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

    fun onCampaignSelected(campaign: Campaign) { uiState.value = uiState.value.copy(selectedCampaign = campaign) }

    fun onAnonymousChange(isAnonymous: Boolean) {
        uiState.value = uiState.value.copy(isAnonymous = isAnonymous, donorName = if(isAnonymous) "" else uiState.value.donorName)
    }

    fun onDonorNameChange(text: String) { uiState.value = uiState.value.copy(donorName = text) }

    // --- LÓGICA DE GUARDAR (CORRIGIDA) ---
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

                // 2. Atualizar Campanha (se aplicável)
                val campaignId = state.selectedCampaign?.docId
                if (!campaignId.isNullOrEmpty()) {
                    db.collection("campaigns").document(campaignId)
                        .update("donations", FieldValue.arrayUnion(newDonationId))
                }

                // 3. Atualizar Stock de Produtos (Agrupado e Seguro)
                updateStockForList(state.productsToAdd, onSuccess)
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro: ${it.message}")
            }
    }

    private fun updateStockForList(products: List<Product>, onComplete: () -> Unit) {
        if (products.isEmpty()) { onComplete(); return }

        // Agrupa produtos pelo nome para evitar conflitos de escrita
        val groupedProducts = products.groupBy { it.name.trim() }

        var processedCount = 0
        val totalGroups = groupedProducts.size

        for ((name, productList) in groupedProducts) {
            val allNewBatches = mutableListOf<ProductBatch>()
            var finalCategory = ""
            var finalImage = ""

            for (p in productList) {
                allNewBatches.addAll(p.batches)
                if (p.category.isNotBlank()) finalCategory = p.category
                if (!p.imageUrl.isNullOrEmpty()) finalImage = p.imageUrl
            }

            // Chama a função de atualização com a lista de lotes consolidada
            updateSingleProductStock(name, finalCategory, allNewBatches, finalImage) {
                processedCount++
                if (processedCount == totalGroups) {
                    uiState.value = uiState.value.copy(isLoading = false, isSuccess = true)
                    onComplete()
                }
            }
        }
    }

    private fun updateSingleProductStock(
        name: String,
        category: String,
        newBatches: List<ProductBatch>,
        img: String?,
        onDone: () -> Unit
    ) {
        // 1. Verifica na lista local se já existe um produto com este nome (ignora maiúsculas/minúsculas)
        val localMatch = uiState.value.existingProducts.find {
            it.name.trim().equals(name.trim(), ignoreCase = true)
        }

        if (localMatch != null && !localMatch.docId.isNullOrEmpty()) {
            // --- O Produto JÁ EXISTE (Encontrado localmente) ---
            val docRef = db.collection("products").document(localMatch.docId)

            docRef.get().addOnSuccessListener { snapshot ->
                val existingProduct = snapshot.toObject(Product::class.java)

                if (existingProduct != null) {
                    val currentBatches = existingProduct.batches

                    // Fundir lotes
                    for (newBatch in newBatches) {
                        var found = false
                        for (existingBatch in currentBatches) {
                            if (isSameDay(existingBatch.validity, newBatch.validity)) {
                                existingBatch.quantity += newBatch.quantity
                                found = true
                                break
                            }
                        }
                        if (!found) {
                            currentBatches.add(newBatch)
                        }
                    }

                    val updates = mutableMapOf<String, Any>("batches" to currentBatches)
                    if (!img.isNullOrEmpty()) updates["imageUrl"] = img
                    // updates["category"] = category // Descomente para atualizar categoria

                    docRef.update(updates).addOnCompleteListener { onDone() }
                } else {
                    // Se falhar a leitura do objeto, tenta criar novo (fallback raro)
                    createNewProduct(name, category, newBatches, img, onDone)
                }
            }.addOnFailureListener {
                onDone()
            }

        } else {
            // --- Produto NÃO encontrado localmente -> Tentar Query Firestore ou Criar Novo ---
            db.collection("products").whereEqualTo("name", name.trim()).get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        // Encontrou na base de dados (lista local podia estar desatualizada)
                        val product = docs.documents[0].toObject(Product::class.java)
                        product?.docId = docs.documents[0].id

                        if (product != null) {
                            // Atualiza lista local e tenta novamente a lógica de update
                            val newList = uiState.value.existingProducts.toMutableList()
                            newList.add(product)
                            uiState.value = uiState.value.copy(existingProducts = newList)

                            // Recursividade segura para ir ao bloco "if (localMatch != null)"
                            updateSingleProductStock(product.name, category, newBatches, img, onDone)
                        } else {
                            onDone()
                        }
                    } else {
                        // Definitivamente não existe -> Criar Novo
                        createNewProduct(name, category, newBatches, img, onDone)
                    }
                }
        }
    }

    private fun createNewProduct(name: String, category: String, newBatches: List<ProductBatch>, img: String?, onDone: () -> Unit) {
        val newItem = Product(
            name = name.trim(),
            category = category,
            imageUrl = img ?: "",
            batches = newBatches.toMutableList()
        )
        db.collection("products").add(newItem).addOnSuccessListener { docRef ->
            // Adiciona logo à lista local para evitar duplicados na mesma sessão
            newItem.docId = docRef.id
            val newList = uiState.value.existingProducts.toMutableList()
            newList.add(newItem)
            uiState.value = uiState.value.copy(existingProducts = newList)

            onDone()
        }
    }

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