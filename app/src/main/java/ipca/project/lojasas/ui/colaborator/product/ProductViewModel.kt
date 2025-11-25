package ipca.project.lojasas.ui.colaborator.product
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
import ipca.project.lojasas.models.ProductBatch
import ipca.project.lojasas.models.StockItem
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Date

data class ProductState(
    var name: String = "",
    var quantity: String = "",
    var validity: Date? = null,
    var imageBase64: String? = null,
    var isLoading: Boolean = false,
    var error: String? = null,
    var isSuccess: Boolean = false
)

class ProductViewModel : ViewModel() {

    var uiState = mutableStateOf(ProductState())
        private set

    private val db = Firebase.firestore

    // Variáveis de controlo para Edição
    private var currentEditingId: String? = null
    private var editingBatchIndex: Int = -1 // Para saber qual lote estamos a editar

    // --- CARREGAR DADOS (Incluindo Stock e Validade) ---
    fun loadProduct(productId: String) {
        if (productId.isBlank()) return

        currentEditingId = productId
        uiState.value = uiState.value.copy(isLoading = true)

        db.collection("products").document(productId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // 1. Converter para o objeto StockItem para aceder aos lotes
                    val item = doc.toObject(StockItem::class.java)
                    val name = item?.name ?: ""
                    val img = item?.imageUrl

                    // 2. Encontrar o lote para exibir (Ex: o que tem a validade mais próxima)
                    var qtyStr = ""
                    var valDate: Date? = null

                    if (item != null && item.batches.isNotEmpty()) {
                        // Ordena por data e pega no primeiro (o que caduca mais cedo)
                        // Ou se preferires o com mais quantidade: sortedByDescending { it.quantity }
                        val batch = item.batches.sortedBy { it.validity }.firstOrNull()

                        if (batch != null) {
                            qtyStr = batch.quantity.toString()
                            valDate = batch.validity
                            // Guardamos o índice original deste lote na lista principal para o podermos atualizar
                            editingBatchIndex = item.batches.indexOf(batch)
                        }
                    }

                    // 3. Preencher a UI
                    uiState.value = uiState.value.copy(
                        name = name,
                        imageBase64 = img,
                        quantity = qtyStr,
                        validity = valDate,
                        isLoading = false
                    )
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Produto não encontrado")
                }
            }
            .addOnFailureListener {
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao carregar: ${it.message}")
            }
    }

    fun onNameChange(text: String) { uiState.value = uiState.value.copy(name = text) }
    fun onQuantityChange(text: String) { if (text.all { it.isDigit() }) uiState.value = uiState.value.copy(quantity = text) }
    fun onDateSelected(date: Date) { uiState.value = uiState.value.copy(validity = date) }

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
            uiState.value = uiState.value.copy(imageBase64 = base64)
        } catch (e: Exception) {
            uiState.value = uiState.value.copy(error = "Erro ao processar imagem")
        }
    }

    fun saveProduct(onSuccess: () -> Unit) {
        val state = uiState.value

        if (state.name.isBlank()) {
            uiState.value = uiState.value.copy(error = "O nome é obrigatório.")
            return
        }

        uiState.value = uiState.value.copy(isLoading = true, error = null)

        val qty = if (state.quantity.isBlank()) 0 else state.quantity.toInt()
        val date = state.validity

        if (currentEditingId != null) {
            // --- MODO EDIÇÃO (Atualizar produto existente) ---
            updateExistingProduct(currentEditingId!!, state.name, qty, date, state.imageBase64, onSuccess)
        } else {
            // --- MODO CRIAÇÃO (Criar novo ou Juntar) ---
            createOrMergeByName(state.name, qty, date, state.imageBase64, onSuccess)
        }
    }

    // --- LÓGICA DE ATUALIZAÇÃO ---
    private fun updateExistingProduct(docId: String, newName: String, qty: Int, date: Date?, img: String?, onSuccess: () -> Unit) {
        val docRef = db.collection("products").document(docId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val stockItem = snapshot.toObject(StockItem::class.java) ?: throw Exception("Produto nulo")

            val batches = stockItem.batches

            // Se carregámos um lote específico (editingBatchIndex != -1), vamos ATUALIZÁ-LO
            // Em vez de somar, vamos substituir os valores, pois o utilizador está a ver o valor atual e a editá-lo.
            if (editingBatchIndex != -1 && editingBatchIndex < batches.size) {
                val batchToEdit = batches[editingBatchIndex]

                // Atualiza a quantidade para o novo valor inserido
                batchToEdit.quantity = qty
                // Atualiza a data
                batchToEdit.validity = date
            } else {
                // Caso de segurança: Se não havia lote, adicionamos um novo
                if (qty > 0) {
                    batches.add(ProductBatch(validity = date, quantity = qty))
                }
            }

            // Prepara updates
            val updates = mutableMapOf<String, Any>(
                "name" to newName,
                "batches" to batches, // Grava a lista atualizada
                "updatedAt" to Date()
            )
            if (img != null) updates["imageUrl"] = img

            transaction.update(docRef, updates)
        }.addOnSuccessListener {
            uiState.value = uiState.value.copy(isLoading = false, isSuccess = true)
            onSuccess()
        }.addOnFailureListener {
            uiState.value = uiState.value.copy(isLoading = false, error = it.message)
        }
    }

    // --- LÓGICA DE CRIAÇÃO (Igual à anterior: Soma se existir) ---
    private fun createOrMergeByName(name: String, qty: Int, date: Date?, img: String?, onSuccess: () -> Unit) {
        if (qty <= 0) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Quantidade obrigatória.")
            return
        }

        db.collection("products").whereEqualTo("name", name.trim()).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    // Já existe -> Soma ao lote (Lógica aditiva)
                    val doc = docs.documents[0]
                    val item = doc.toObject(StockItem::class.java)!!
                    val batches = item.batches

                    var found = false
                    for (b in batches) {
                        if (isSameDay(b.validity, date)) {
                            b.quantity += qty // Aqui SOMAMOS porque é "Adicionar"
                            found = true; break
                        }
                    }
                    if (!found) batches.add(ProductBatch(validity = date, quantity = qty))

                    doc.reference.update("batches", batches)
                        .addOnSuccessListener { onSuccess() }
                } else {
                    // Novo
                    val newBatch = ProductBatch(validity = date, quantity = qty)
                    val newItem = StockItem(name = name, imageUrl = img ?: "", batches = mutableListOf(newBatch))
                    db.collection("products").add(newItem).addOnSuccessListener { onSuccess() }
                }
            }
    }

    // ... (Funções utilitárias mantêm-se) ...
    private fun isSameDay(date1: Date?, date2: Date?): Boolean {
        if (date1 == null && date2 == null) return true
        if (date1 == null || date2 == null) return false
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width; val originalHeight = bitmap.height
        var resizedWidth = maxDimension; var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension; resizedWidth = (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension; resizedHeight = (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else { if (originalHeight < maxDimension) return bitmap }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}