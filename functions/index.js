const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

// ==================================================================
// FUNÇÃO 1: AVISA COLABORADORES DE NOVA CANDIDATURA (onCreate)
// ==================================================================
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

      querySnapshot.forEach(doc => {
        const adminData = doc.data();
        const adminId = doc.id;

        if (adminData.fcmToken) {
          tokensParaEnvio.push(adminData.fcmToken);
        }

        const notifRef = admin.firestore().collection('notifications').doc();

        batch.set(notifRef, {
          recipientId: adminId,
          title: "Nova Candidatura 📄",
          body: `${nomeDoAluno} submeteu uma nova candidatura.`,
          date: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
          type: "candidatura_nova",
          relatedId: candidaturaId,
          forCollaborator: true // Para o ADMIN ver
        });
      });

      await batch.commit();

      if (tokensParaEnvio.length > 0) {
        const message = {
          notification: {
            title: "Nova Candidatura 📄",
            body: `${nomeDoAluno} submeteu uma nova candidatura.`
          },
          tokens: tokensParaEnvio
        };
        await admin.messaging().sendEachForMulticast(message);
      }

      return null;

    } catch (error) {
      console.error("Erro ao processar notificação nova:", error);
      return null;
    }
  });


// ==================================================================
// FUNÇÃO 2: AVISA O ALUNO QUANDO O ESTADO MUDA (onUpdate)
// ==================================================================
exports.notificarMudancaEstado = functions.firestore
  .document('candidatures/{candidaturaId}')
  .onUpdate(async (change, context) => {
    
    // Pegar dados de ANTES e DEPOIS da mudança
    const dadosNovos = change.after.data();
    const dadosAntigos = change.before.data();
    const candidaturaId = context.params.candidaturaId;

    // Se o estado não mudou, não fazemos nada (evita loops infinitos)
    if (dadosNovos.state === dadosAntigos.state) {
      return null;
    }

    const novoEstado = dadosNovos.state; // Ex: 'ACEITE', 'REJEITADA'
    const alunoId = dadosNovos.userId;

    // Verifica se temos ID do aluno para enviar
    if (!alunoId) {
      console.log("Candidatura sem userId, impossível notificar aluno.");
      return null;
    }

    try {
      // 1. Definir Título e Mensagem com base no Estado
      let titulo = "";
      let corpo = "";

      if (novoEstado === "ACEITE") {
        titulo = "Candidatura Aceite ✅";
        corpo = "Boas notícias! A sua candidatura foi validada pela equipa.";
      } else if (novoEstado === "REJEITADA") {
        titulo = "Atualização da Candidatura ⚠️";
        corpo = "O estado da sua candidatura foi alterado para REJEITADA. Verifique os detalhes.";
      } else {
        // Se mudou para outra coisa qualquer (ex: PENDENTE novamente), ignoramos
        return null;
      }

      // 2. Buscar o Token do Aluno
      const userDoc = await admin.firestore().collection('users').doc(alunoId).get();
      
      if (!userDoc.exists) {
        console.log("Utilizador não encontrado na BD.");
        return null;
      }

      const userData = userDoc.data();
      const alunoToken = userData.fcmToken;

      // 3. Guardar no Histórico (Firestore)
      // Nota: Aqui não usamos batch porque é só para UMA pessoa
      await admin.firestore().collection('notifications').add({
        recipientId: alunoId,
        title: titulo,
        body: corpo,
        date: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
        type: "candidatura_estado",
        relatedId: candidaturaId,
        forCollaborator: false // FALSE = Para o beneficiário ver
      });

      console.log(`Notificação de estado (${novoEstado}) salva para o aluno.`);

      // 4. Enviar Push Notification (se tiver token)
      if (alunoToken) {
        const message = {
          notification: {
            title: titulo,
            body: corpo
          },
          token: alunoToken // Note: aqui é 'token' (singular) porque é send() simples, não multicast
        };

        await admin.messaging().send(message);
        console.log("Push enviado para o aluno.");
      } else {
        console.log("Aluno sem token, apenas histórico salvo.");
      }

      return null;

    } catch (error) {
      console.error("Erro ao notificar mudança de estado:", error);
      return null;
    }
  });