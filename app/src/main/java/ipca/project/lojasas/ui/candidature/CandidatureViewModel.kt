package ipca.project.lojasas.ui.candidature

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.project.lojasas.models.Candidature
import ipca.project.lojasas.models.DocumentAttachment
import ipca.project.lojasas.models.CandidatureState // O Enum do modelo (PENDING, etc)
import ipca.project.lojasas.models.Type // O Enum do modelo (STUDENT, EMPLOYEE)
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Renomeei para UiState para evitar confusão com o Enum 'CandidatureState' do modelo
data class CandidatureUiState (
    var candidature: Candidature = Candidature(),
    var error: String? = null,
    var isLoading: Boolean = false,
    var isSubmitted: Boolean = false
)

class CandidatureViewModel : ViewModel() {

    var uiState = mutableStateOf(CandidatureUiState())
        private set

    // --- PERSONAL DATA ---

    fun updateAcademicYear(input: String) {
        val digits = input.filter { it.isDigit() }.take(8)
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(academicYear = digits)
        )
    }

    fun updateBirthDate(input: String) {
        val digits = input.filter { it.isDigit() }.take(8)
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(birthDate = digits)
        )
    }

    fun updateMobilePhone(phone: String) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(mobilePhone = phone)
        )
    }

    fun updateEmail(email: String) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(email = email)
        )
    }

    // --- ACADEMIC DATA ---

    fun updateType(newType: Type) {
        // Se mudar o tipo, convém limpar o erro se existir
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(type = newType),
            error = null
        )
    }

    fun updateCourse(course: String) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(course = course)
        )
    }

    fun updateCardNumber(number: String) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(cardNumber = number)
        )
    }

    // --- PRODUCTS ---

    fun updateFoodProducts(value: Boolean) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(foodProducts = value)
        )
    }

    fun updateHygieneProducts(value: Boolean) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(hygieneProducts = value)
        )
    }

    fun updateCleaningProducts(value: Boolean) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(cleaningProducts = value)
        )
    }

    // --- SUPPORTS ---

    fun updateFaesSupport(value: Boolean) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(faesSupport = value)
        )
    }

    fun updateScholarshipSupport(value: Boolean) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(scholarshipSupport = value)
        )
    }

    fun updateScholarshipDetails(text: String) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(scholarshipDetails = text)
        )
    }

    // --- ATTACHMENTS ---

    fun addAttachment(fileName: String, base64: String) {
        val currentList = uiState.value.candidature.attachments.toMutableList()
        currentList.add(DocumentAttachment(fileName, base64))
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(attachments = currentList)
        )
    }

    fun removeAttachment(index: Int) {
        val currentList = uiState.value.candidature.attachments.toMutableList()
        if (index >= 0 && index < currentList.size) {
            currentList.removeAt(index)
            uiState.value = uiState.value.copy(
                candidature = uiState.value.candidature.copy(attachments = currentList)
            )
        }
    }

    // --- FINALIZATION ---

    fun updateTruthfulnessDeclaration(value: Boolean) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(truthfulnessDeclaration = value)
        )
    }

    fun updateDataAuthorization(value: Boolean) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(dataAuthorization = value)
        )
    }

    fun updateSignature(signature: String) {
        uiState.value = uiState.value.copy(
            candidature = uiState.value.candidature.copy(signature = signature)
        )
    }

    // --- VALIDATION ---

    private fun isFormValid(): Boolean {
        val c = uiState.value.candidature

        val isAcademicYearOk = c.academicYear.length == 8
        val isBirthDateOk = c.birthDate.length == 8

        val anyProduct = c.foodProducts || c.hygieneProducts || c.cleaningProducts
        val isCourseValid = if (c.type == Type.FUNCIONARIO) true else !c.course.isNullOrBlank()

        return isAcademicYearOk &&
                isBirthDateOk &&
                c.mobilePhone.isNotBlank() &&
                c.email.isNotBlank() &&
                c.type != null &&
                isCourseValid &&
                c.cardNumber.isNotBlank() &&
                anyProduct &&
                c.faesSupport != null &&
                c.scholarshipSupport != null &&
                c.truthfulnessDeclaration &&
                c.dataAuthorization &&
                // A verificação 'isSignatureDateOk' foi removida daqui
                c.signature.isNotBlank()
    }


    // --- SUBMISSION ---
    fun submitCandidature(onSubmitResult: (Boolean) -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        if (!isFormValid()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Por favor, preenche todos os campos obrigatórios."
            )
            onSubmitResult(false)
            return
        }

        // ... (verificação do user e inicialização do Firebase)
        val user = Firebase.auth.currentUser
        if (user == null) {
            // ... (lógica de erro)
            return
        }
        val uid = user.uid

        val db = Firebase.firestore
        val batch = db.batch()

        val newCandidatureRef = db.collection("candidatures").document()
        val generatedId = newCandidatureRef.id

        val c = uiState.value.candidature

        // --- ALTERAÇÃO PRINCIPAL AQUI ---
        // 1. Obter a data e hora atuais
        val currentDate = Date()

        // 2. Formatar a data atual para o formato "dd/MM/yyyy"
        val signatureDateFormatter = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )
        val formattedSignatureDate = signatureDateFormatter.format(currentDate)

        // Funções locais de formatação para os outros campos
        fun formatDate(s: String) = if(s.length == 8) "${s.substring(0,2)}/${s.substring(2,4)}/${s.substring(4,8)}" else s
        fun formatYear(s: String) = if(s.length == 8) "${s.substring(0,4)}/${s.substring(4,8)}" else s

        val candidatureToSend = c.copy(
            docId = generatedId,
            userId = uid,
            academicYear = formatYear(c.academicYear),
            birthDate = formatDate(c.birthDate),
            // 3. Usar a data formatada e a data completa do sistema
            signatureDate = formattedSignatureDate, // Data formatada para visualização
            creationDate = currentDate, // Data e hora exatas da criação
            updateDate = currentDate,   // Data e hora exatas da atualização inicial
            state = CandidatureState.PENDENTE
        )

        batch.set(newCandidatureRef, candidatureToSend)

        val userRef = db.collection("users").document(uid)
        batch.update(userRef, "candidatureId", generatedId)

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