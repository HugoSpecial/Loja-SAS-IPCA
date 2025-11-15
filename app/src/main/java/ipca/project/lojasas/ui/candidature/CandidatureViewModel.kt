package ipca.project.lojasas.ui.candidature

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import ipca.example.lojasas.models.Candidatura
import ipca.example.lojasas.models.EstadoCandidatura
import ipca.example.lojasas.models.TipoCurso
import ipca.project.lojasas.TAG
import java.util.Date

data class CandidatureState (
    var candidatura: Candidatura = Candidatura(),
    var error: String? = null,
    var isLoading: Boolean = false,
    var isSubmitted: Boolean = false
)

class CandidatureViewModel : ViewModel() {

    var uiState = mutableStateOf(CandidatureState())
        private set

    // Identificação do candidato
    fun updateAnoLetivo(anoLetivo: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(anoLetivo = anoLetivo)
        )
    }

    fun updateDataNascimento(dataNascimento: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(dataNascimento = dataNascimento)
        )
    }

    fun updateTelemovel(telemovel: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(telemovel = telemovel)
        )
    }

    fun updateEmail(email: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(email = email)
        )
    }

    // Dados académicos
    fun updateTipoCurso(tipoCurso: String) {
        val tipo = when (tipoCurso) {
            "Licenciatura" -> TipoCurso.LICENCIATURA
            "Mestrado" -> TipoCurso.MESTRADO
            "CTeSP" -> TipoCurso.CTESP
            else -> null
        }
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(tipoCurso = tipo)
        )
    }

    fun updateCurso(curso: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(curso = curso)
        )
    }

    fun updateNumeroEstudante(numeroEstudante: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(numeroEstudante = numeroEstudante)
        )
    }

    // Tipologia do pedido
    fun updateProdutosAlimentares(produtosAlimentares: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(produtosAlimentares = produtosAlimentares)
        )
    }

    fun updateProdutosHigiene(produtosHigiene: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(produtosHigiene = produtosHigiene)
        )
    }

    fun updateProdutosLimpeza(produtosLimpeza: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(produtosLimpeza = produtosLimpeza)
        )
    }

    // Outros apoios
    fun updateFaesApoiado(faesApoiado: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(faesApoiado = faesApoiado)
        )
    }

    fun updateBolsaApoio(bolsaApoio: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(bolsaApoio = bolsaApoio)
        )
    }

    fun updateDetalhesBolsa(detalhesBolsa: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(detalhesBolsa = detalhesBolsa)
        )
    }

    // Declarações
    fun updateDeclaracaoVeracidade(declaracaoVeracidade: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(declaracaoVeracidade = declaracaoVeracidade)
        )
    }

    fun updateAutorizacaoDados(autorizacaoDados: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(autorizacaoDados = autorizacaoDados)
        )
    }

    // Data e assinatura
    fun updateDataAssinatura(dataAssinatura: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(dataAssinatura = dataAssinatura)
        )
    }

    fun updateAssinatura(assinatura: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(assinatura = assinatura)
        )
    }

    // Validação do formulário
    private fun isFormValid(): Boolean {
        val candidatura = uiState.value.candidatura
        return candidatura.anoLetivo.isNotBlank() &&
                candidatura.dataNascimento.isNotBlank() &&
                candidatura.telemovel.isNotBlank() &&
                candidatura.email.isNotBlank() &&
                candidatura.tipoCurso != null &&
                candidatura.curso.isNotBlank() &&
                candidatura.numeroEstudante.isNotBlank() &&
                (candidatura.produtosAlimentares || candidatura.produtosHigiene || candidatura.produtosLimpeza) &&
                candidatura.faesApoiado != null &&
                candidatura.bolsaApoio != null &&
                candidatura.declaracaoVeracidade &&
                candidatura.autorizacaoDados &&
                candidatura.dataAssinatura.isNotBlank() &&
                candidatura.assinatura.isNotBlank()
    }

    // Submissão da candidatura
    fun submitCandidatura(onSubmitResult: (Boolean) -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        if (!isFormValid()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Por favor, preencha todos os campos obrigatórios"
            )
            onSubmitResult(false)
            return
        }

        // Preparar dados para Firebase
        val candidatura = uiState.value.candidatura.copy(
            dataCriacao = Date(),
            dataAtualizacao = Date(),
            estado = EstadoCandidatura.PENDENTE
            // userId será definido quando tiver autenticação
        )

        val db = Firebase.firestore

        db.collection("candidaturas")
            .add(candidatura)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Candidatura submetida com ID: ${documentReference.id}")
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    isSubmitted = true,
                    error = null
                )
                onSubmitResult(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Erro ao submeter candidatura", e)
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao submeter candidatura. Tente novamente."
                )
                onSubmitResult(false)
            }
    }



    // Limpar estado
    fun clearError() {
        uiState.value = uiState.value.copy(error = null)
    }

    fun resetState() {
        uiState.value = CandidatureState()
    }
}