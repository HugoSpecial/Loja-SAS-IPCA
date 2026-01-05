package ipca.project.lojasas.ui.candidature

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.DocumentAttachment
import ipca.project.lojasas.models.CandidatureState
import ipca.project.lojasas.models.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CandidatureUiState (
    var candidature: Candidature = Candidature(),
    var error: String? = null,
    var isLoading: Boolean = false,
    var isSubmitted: Boolean = false
)

class CandidatureViewModel : ViewModel() {

    var uiState = mutableStateOf(CandidatureUiState())
        private set

    // --- GESTÃO DE ESTADO (NOVO/EDITAR) ---

    fun resetState() {
        uiState.value = CandidatureUiState()
    }

    fun loadCandidature(candidatureId: String) {
        uiState.value = uiState.value.copy(isLoading = true)

        // --- ADICIONA ESTES LOGS ---
        android.util.Log.d("DEBUG_IPCA", "A tentar ler ID: '$candidatureId'")
        android.util.Log.d("DEBUG_IPCA", "Tamanho do ID: ${candidatureId.length}")
        // ---------------------------

        val db = Firebase.firestore
        db.collection("candidatures").document(candidatureId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        // O nome dos campos no Firebase (Inglês) bate certo com o Model
                        val loadedCandidature = document.toObject(Candidature::class.java)

                        if (loadedCandidature != null) {
                            // LIMPEZA CRUCIAL:
                            // O Firebase tem "2024/2025", mas o input só aceita "20242025"
                            val cleanYear = loadedCandidature.academicYear.replace("/", "")
                            val cleanBirth = loadedCandidature.birthDate.replace("/", "")

                            uiState.value = uiState.value.copy(
                                candidature = loadedCandidature.copy(
                                    academicYear = cleanYear,
                                    birthDate = cleanBirth,
                                    docId = document.id // Garante que o ID fica guardado para edição
                                ),
                                isLoading = false,
                                error = null
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao processar dados: ${e.message}. Verifica os Enums."
                        )
                    }
                } else {
                    uiState.value = uiState.value.copy(isLoading = false, error = "Candidatura não encontrada.")
                }
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro de rede: ${e.message}")
            }
    }

    // --- ATUALIZAÇÃO DOS CAMPOS (INPUTS) ---

    fun updateAcademicYear(input: String) {
        val digits = input.filter { it.isDigit() }.take(8)
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(academicYear = digits))
    }

    fun updateBirthDate(input: String) {
        val digits = input.filter { it.isDigit() }.take(8)
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(birthDate = digits))
    }

    fun updateMobilePhone(phone: String) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(mobilePhone = phone))
    }

    fun updateEmail(email: String) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(email = email))
    }

    fun updateType(newType: Type) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(type = newType), error = null)
    }

    fun updateCourse(course: String) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(course = course))
    }

    fun updateCardNumber(number: String) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(cardNumber = number))
    }

    fun updateFoodProducts(value: Boolean) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(foodProducts = value))
    }

    fun updateHygieneProducts(value: Boolean) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(hygieneProducts = value))
    }

    fun updateCleaningProducts(value: Boolean) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(cleaningProducts = value))
    }

    fun updateFaesSupport(value: Boolean) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(faesSupport = value))
    }

    fun updateScholarshipSupport(value: Boolean) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(scholarshipSupport = value))
    }

    fun updateScholarshipDetails(text: String) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(scholarshipDetails = text))
    }

    fun addAttachment(fileName: String, base64: String) {
        val currentList = uiState.value.candidature.attachments.toMutableList()
        currentList.add(DocumentAttachment(fileName, base64))
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(attachments = currentList))
    }

    fun removeAttachment(index: Int) {
        val currentList = uiState.value.candidature.attachments.toMutableList()
        if (index >= 0 && index < currentList.size) {
            currentList.removeAt(index)
            uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(attachments = currentList))
        }
    }

    fun updateTruthfulnessDeclaration(value: Boolean) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(truthfulnessDeclaration = value))
    }

    fun updateDataAuthorization(value: Boolean) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(dataAuthorization = value))
    }

    fun updateSignature(signature: String) {
        uiState.value = uiState.value.copy(candidature = uiState.value.candidature.copy(signature = signature))
    }

    // --- VALIDAÇÃO ---

    private fun isFormValid(): Boolean {
        val c = uiState.value.candidature
        val isAcademicYearOk = c.academicYear.length == 8
        val isBirthDateOk = c.birthDate.length == 8
        val anyProduct = c.foodProducts || c.hygieneProducts || c.cleaningProducts
        val isCourseValid = if (c.type == Type.FUNCIONARIO) true else !c.course.isNullOrBlank()

        return isAcademicYearOk && isBirthDateOk && c.mobilePhone.isNotBlank() &&
                c.email.isNotBlank() && c.type != null && isCourseValid &&
                c.cardNumber.isNotBlank() && anyProduct && c.faesSupport != null &&
                c.scholarshipSupport != null && c.truthfulnessDeclaration &&
                c.dataAuthorization && c.signature.isNotBlank()
    }

    // --- SUBMISSÃO (CRIAR E ATUALIZAR) ---

    fun submitCandidature(onSubmitResult: (Boolean) -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        if (!isFormValid()) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Por favor, preenche todos os campos obrigatórios.")
            onSubmitResult(false)
            return
        }

        val user = Firebase.auth.currentUser
        if (user == null) {
            uiState.value = uiState.value.copy(isLoading = false, error = "Utilizador não autenticado.")
            onSubmitResult(false)
            return
        }

        val db = Firebase.firestore
        val batch = db.batch()
        val c = uiState.value.candidature

        // Verifica se é Edição (tem docId) ou Criação (não tem)
        val isEditing = !c.docId.isNullOrEmpty()

        val docRef = if (isEditing) {
            db.collection("candidatures").document(c.docId!!)
        } else {
            db.collection("candidatures").document()
        }

        val currentDate = Date()
        val signatureDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedSignatureDate = signatureDateFormatter.format(currentDate)

        // Formata as datas para adicionar as barras antes de enviar
        fun formatDate(s: String) = if(s.length == 8) "${s.substring(0,2)}/${s.substring(2,4)}/${s.substring(4,8)}" else s
        fun formatYear(s: String) = if(s.length == 8) "${s.substring(0,4)}/${s.substring(4,8)}" else s

        val candidatureToSend = c.copy(
            docId = docRef.id,
            userId = user.uid,
            academicYear = formatYear(c.academicYear),
            birthDate = formatDate(c.birthDate),
            signatureDate = formattedSignatureDate,
            // Se editar, mantém a creationDate antiga. Se novo, usa a atual.
            creationDate = if (isEditing) c.creationDate else currentDate,
            updateDate = currentDate,
            // Ao corrigir, volta sempre para PENDENTE
            state = CandidatureState.PENDENTE
        )

        batch.set(docRef, candidatureToSend)

        // Atualiza a referência no User (redundante na edição, mas seguro)
        val userRef = db.collection("users").document(user.uid)
        batch.update(userRef, "candidatureId", docRef.id)

        batch.commit()
            .addOnSuccessListener {
                uiState.value = uiState.value.copy(isLoading = false, isSubmitted = true, error = null)
                onSubmitResult(true)
            }
            .addOnFailureListener { e ->
                uiState.value = uiState.value.copy(isLoading = false, error = "Erro ao submeter: ${e.message}")
                onSubmitResult(false)
            }
    }
}