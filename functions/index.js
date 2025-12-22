const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

exports.notificarNovaCandidatura = functions.firestore
  .document('candidatures/{candidaturaId}') 
  .onCreate(async (snap, context) => {
    
    const dadosCandidatura = snap.data();
    const candidaturaId = context.params.candidaturaId;
    
    let nomeDoAluno = "Interessado"; 

    try {
      if (dadosCandidatura.userId) {
        const userDoc = await admin.firestore().collection('users').doc(dadosCandidatura.userId).get();
        
        if (userDoc.exists) {
          const userData = userDoc.data();
          nomeDoAluno = userData.name || userData.nome || "Aluno desconhecido";
        }
      } else {
        nomeDoAluno = dadosCandidatura.name || dadosCandidatura.nome || "Um aluno";
      }

      console.log(`Nova candidatura de: ${nomeDoAluno}`);

      // --- PASSO 2: ENCONTRAR COLABORADORES ---
      const querySnapshot = await admin.firestore()
        .collection('users')
        .where('isCollaborator', '==', true) 
        .get();

      if (querySnapshot.empty) {
        console.log('Nenhum colaborador encontrado.');
        return null;
      }

      const batch = admin.firestore().batch();
      const tokensParaEnvio = [];

      // --- PASSO 3: PREPARAR NOTIFICAÇÕES ---
      querySnapshot.forEach(doc => {
        const adminData = doc.data();
        const adminId = doc.id;

        if (adminData.fcmToken) {
          tokensParaEnvio.push(adminData.fcmToken);
        }

        // Criar registo na coleção 'notifications'
        const notifRef = admin.firestore().collection('notifications').doc();

        batch.set(notifRef, {
          recipientId: adminId,
          title: "Nova Candidatura",
          // AQUI ESTÁ A MUDANÇA: O corpo da mensagem agora tem o nome real
          body: `${nomeDoAluno} submeteu um nova candidatura.`,
          date: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
          type: "candidatura_nova",
          relatedId: candidaturaId,
          forCollaborator: true 
        });
      });

      // Gravar histórico
      await batch.commit();

      // --- PASSO 4: ENVIAR PUSH PARA O TELEMÓVEL ---
      if (tokensParaEnvio.length > 0) {
        const message = {
          notification: {
            title: "Nova Candidatura",
            // O nome também aparece no telemóvel
            body: `${nomeDoAluno} submeteu um nova candidatura.`
          },
          tokens: tokensParaEnvio
        };

        await admin.messaging().sendEachForMulticast(message);
      }

      return null;

    } catch (error) {
      console.error("Erro ao processar notificação:", error);
      return null;
    }
  });