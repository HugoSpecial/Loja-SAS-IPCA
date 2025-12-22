const functions = require("firebase-functions/v1"); 
const admin = require("firebase-admin");

admin.initializeApp();

exports.notificarNovaCandidatura = functions.firestore
  .document('candidatures/{docId}')
  .onCreate(async (snap, context) => {
    
    const dados = snap.data();
    const nome = dados.nome || "Um beneficiário"; 

    try {
      // Procura Colaboradores
      const querySnapshot = await admin.firestore()
        .collection('users')
        .where('isCollaborator', '==', true) 
        .get();

      if (querySnapshot.empty) {
        console.log('Nenhum colaborador encontrado.');
        return null;
      }

      // Junta tokens
      const tokens = [];
      querySnapshot.forEach(doc => {
        const userData = doc.data();
        if (userData.fcmToken) {
          tokens.push(userData.fcmToken);
        }
      });

      if (tokens.length === 0) {
        console.log('Sem tokens para enviar.');
        return null;
      }

      // Prepara mensagem
      const message = {
        notification: {
          title: "Nova Candidatura Recebida",
          body: `${nome} enviou um novo pedido.`
        },
        tokens: tokens
      };

      // Envia
      const response = await admin.messaging().sendEachForMulticast(message);
      console.log('Sucesso:', response.successCount);
      
      return null;

    } catch (error) {
      console.error("Erro:", error);
      return null;
    }
  });