package ipca.project.lojasas

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Este método é chamado automaticamente quando um NOVO token é gerado
    // (ex: primeira vez que instala a app ou quando o token expira)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Novo token gerado: $token")

        // Se o user já estiver logado, atualizamos logo na base de dados
        enviarTokenParaFirestore(token)
    }

    // Este método é chamado quando recebes uma notificação e a app está ABERTA
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Mensagem recebida de: ${remoteMessage.from}")

        // Aqui podes decidir mostrar um alerta visual na app se quiseres
        // Se a app estiver fechada, o Android trata disto sozinho e mostra na barra de topo
    }

    private fun enviarTokenParaFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(user.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token atualizado no Firestore para o user ${user.uid}")
                }
                .addOnFailureListener {
                    Log.e("FCM", "Erro ao atualizar token", it)
                }
        }
    }
}