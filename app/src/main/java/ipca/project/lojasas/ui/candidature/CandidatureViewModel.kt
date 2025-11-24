package ipca.project.lojasas.ui.candidature

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import ipca.example.lojasas.models.Candidatura
import ipca.example.lojasas.models.DocumentoAnexo
import ipca.example.lojasas.models.EstadoCandidatura
import ipca.example.lojasas.models.Tipo
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

    fun updateAnoLetivo(input: String) {
        val digits = input.filter { it.isDigit() }.take(8)
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(anoLetivo = digits)
        )
    }

    fun updateDataNascimento(input: String) {
        val digits = input.filter { it.isDigit() }.take(8)
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(dataNascimento = digits)
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

    fun updateTipo(novoTipo: Tipo) {
        // Se mudar o tipo, convém limpar o erro se existir
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(tipo = novoTipo),
            error = null
        )
    }

    fun updateCurso(curso: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(curso = curso)
        )
    }

    fun updateNumeroCartao(numero: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(numeroCartao = numero)
        )
    }

    fun updateProdutosAlimentares(valor: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(produtosAlimentares = valor)
        )
    }

    fun updateProdutosHigiene(valor: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(produtosHigiene = valor)
        )
    }

    fun updateProdutosLimpeza(valor: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(produtosLimpeza = valor)
        )
    }

    fun updateFaesApoiado(valor: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(faesApoiado = valor)
        )
    }

    fun updateBolsaApoio(valor: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(bolsaApoio = valor)
        )
    }

    fun updateDetalhesBolsa(texto: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(detalhesBolsa = texto)
        )
    }

    fun addAnexo(nomeFicheiro: String, base64: String) {
        val listaAtual = uiState.value.candidatura.anexos.toMutableList()
        listaAtual.add(DocumentoAnexo(nomeFicheiro, base64))
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(anexos = listaAtual)
        )
    }

    fun removeAnexo(index: Int) {
        val listaAtual = uiState.value.candidatura.anexos.toMutableList()
        if (index >= 0 && index < listaAtual.size) {
            listaAtual.removeAt(index)
            uiState.value = uiState.value.copy(
                candidatura = uiState.value.candidatura.copy(anexos = listaAtual)
            )
        }
    }

    fun updateDeclaracaoVeracidade(valor: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(declaracaoVeracidade = valor)
        )
    }

    fun updateAutorizacaoDados(valor: Boolean) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(autorizacaoDados = valor)
        )
    }

    fun updateDataAssinatura(input: String) {
        val digits = input.filter { it.isDigit() }.take(8)
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(dataAssinatura = digits)
        )
    }

    fun updateAssinatura(assinatura: String) {
        uiState.value = uiState.value.copy(
            candidatura = uiState.value.candidatura.copy(assinatura = assinatura)
        )
    }

    private fun isFormValid(): Boolean {
        val c = uiState.value.candidatura

        val isAnoLetivoOk = c.anoLetivo.length == 8
        val isDataNascOk = c.dataNascimento.length == 8
        val isDataAssinaturaOk = c.dataAssinatura.length == 8

        // Verifica se a lista de produtos tem pelo menos um selecionado
        val algumProduto = c.produtosAlimentares || c.produtosHigiene || c.produtosLimpeza

        // Se NÃO for funcionário, o curso é obrigatório
        val cursoValido = if (c.tipo == Tipo.FUNCIONARIO) true else !c.curso.isNullOrBlank()

        return isAnoLetivoOk &&
                isDataNascOk &&
                c.telemovel.isNotBlank() &&
                c.email.isNotBlank() &&
                c.tipo != null &&
                cursoValido &&
                c.numeroCartao.isNotBlank() &&
                algumProduto &&
                c.faesApoiado != null &&
                c.bolsaApoio != null &&
                c.declaracaoVeracidade &&
                c.autorizacaoDados &&
                isDataAssinaturaOk &&
                c.assinatura.isNotBlank()
    }

    fun submitCandidatura(onSubmitResult: (Boolean) -> Unit) {
        uiState.value = uiState.value.copy(isLoading = true)

        // 1. Validação
        if (!isFormValid()) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Por favor, preenche todos os campos obrigatórios."
            )
            onSubmitResult(false)
            return
        }

        val user = Firebase.auth.currentUser
        if (user == null) {
            uiState.value = uiState.value.copy(
                isLoading = false,
                error = "Sessão inválida. Por favor, faz login novamente."
            )
            onSubmitResult(false)
            return
        }
        val uid = user.uid

        val db = Firebase.firestore
        val batch = db.batch()

        // 4. Gerar o ID da nova candidatura (sem gravar ainda)
        val novaCandidaturaRef = db.collection("candidatures").document()
        val idGerado = novaCandidaturaRef.id

        // 5. Preparar o Objeto Candidatura
        val c = uiState.value.candidatura
        fun formatData(s: String) = if(s.length == 8) "${s.substring(0,2)}/${s.substring(2,4)}/${s.substring(4,8)}" else s
        fun formatAno(s: String) = if(s.length == 8) "${s.substring(0,4)}/${s.substring(4,8)}" else s

        val candidaturaParaEnvio = c.copy(
            docId = idGerado,          // Define o ID no objeto Candidatura
            userId = uid,              // Define o ID do User na Candidatura (Ligação User -> Cand)
            anoLetivo = formatAno(c.anoLetivo),
            dataNascimento = formatData(c.dataNascimento),
            dataAssinatura = formatData(c.dataAssinatura),
            dataCriacao = Date(),
            dataAtualizacao = Date(),
            estado = EstadoCandidatura.PENDENTE
        )

        // 6. ADICIONAR AO BATCH: Gravar a Candidatura
        batch.set(novaCandidaturaRef, candidaturaParaEnvio)

        // 7. ADICIONAR AO BATCH: Atualizar o User com o ID da Candidatura
        val userRef = db.collection("users").document(uid)

        // Aqui atualizamos o campo 'candidature' do User com o 'docId' da Candidatura
        batch.update(userRef, "candidatureId", idGerado)

        // 8. Executar tudo atomicamente
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