const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

// ==================================================================
// 1. NOVA CANDIDATURA (Ação do INTERESSADO -> Avisa COLABORADOR)
// ==================================================================
exports.notificarNovaCandidatura = functions.firestore
  .document('candidatures/{candidaturaId}') 
  .onCreate(async (snap, context) => {
    
    const dados = snap.data();
    const candidaturaId = context.params.candidaturaId;
    let nomeDoAluno = "Interessado"; 

    try {
      if (dados.userId) {
        const userDoc = await admin.firestore().collection('users').doc(dados.userId).get();
        if (userDoc.exists) {
          const userData = userDoc.data();
          nomeDoAluno = userData.name || userData.nome || "Aluno desconhecido";
        }
      } else {
        nomeDoAluno = dados.name || dados.nome || "Um aluno";
      }

      // Procurar Colaboradores
      const querySnapshot = await admin.firestore()
        .collection('users')
        .where('isCollaborator', '==', true) 
        .get();

      if (querySnapshot.empty) return null;

      const batch = admin.firestore().batch();
      const tokens = [];

      querySnapshot.forEach(doc => {
        const adminData = doc.data();
        if (adminData.fcmToken) tokens.push(adminData.fcmToken);

        const notifRef = admin.firestore().collection('notifications').doc();
        batch.set(notifRef, {
          recipientId: doc.id,
          title: "Nova Candidatura",
          body: `${nomeDoAluno} submeteu um novo pedido.`,
          date: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
          type: "candidatura_nova",
          relatedId: candidaturaId,
          receiverId: "collaborator"
        });
      });

      await batch.commit();

      if (tokens.length > 0) {
        await admin.messaging().sendEachForMulticast({
          notification: { 
            title: "Nova Candidatura", 
            body: `${nomeDoAluno} submeteu um novo pedido.` 
          },
          tokens: tokens
        });
      }
      return null;
    } catch (error) { 
      console.error("Erro nova candidatura:", error);
      return null; 
    }
  });


// ==================================================================
// 2. MUDANÇA ESTADO CANDIDATURA (Ação do COLABORADOR -> Avisa INTERESSADO)
// ==================================================================
exports.notificarMudancaEstado = functions.firestore
  .document('candidatures/{candidaturaId}')
  .onUpdate(async (change, context) => {
    
    const dadosNovos = change.after.data();
    const dadosAntigos = change.before.data();
    const candidaturaId = context.params.candidaturaId;

    if (dadosNovos.state === dadosAntigos.state) return null;
    if (!dadosNovos.userId) return null;

    let titulo = "";
    let corpo = "";

    if (dadosNovos.state === "ACEITE") {
      titulo = "Candidatura Aceite";
      corpo = "A sua candidatura foi aceite. Bem-vindo!";
    } else if (dadosNovos.state === "RECUSADA") {
      titulo = "Candidatura Recusada";
      corpo = "Verifique os motivos na aplicacao.";
    } else {
      return null;
    }

    try {
      const userDoc = await admin.firestore().collection('users').doc(dadosNovos.userId).get();
      const userToken = userDoc.exists ? userDoc.data().fcmToken : null;

      await admin.firestore().collection('notifications').add({
        recipientId: dadosNovos.userId,
        title: titulo,
        body: corpo,
        date: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
        type: "candidatura_estado",
        relatedId: candidaturaId,
        targetProfile: "INTERESSADO"
      });

      if (userToken) {
        await admin.messaging().send({
          notification: { title: titulo, body: corpo },
          token: userToken
        });
      }
      return null;
    } catch (error) { 
      console.error("Erro estado candidatura:", error);
      return null; 
    }
  });


// ==================================================================
// 3. NOVO PEDIDO CABAZ (Ação do BENEFICIARIO -> Avisa COLABORADOR)
// ==================================================================
exports.notificarNovoPedido = functions.firestore
  .document('orders/{orderId}')
  .onCreate(async (snap, context) => {
    
    const dados = snap.data();
    const nomeAluno = dados.userName || "Um beneficiario";
    const orderId = context.params.orderId;

    try {
      const colabs = await admin.firestore().collection('users').where('isCollaborator', '==', true).get();
      if (colabs.empty) return null;

      const batch = admin.firestore().batch();
      const tokens = [];

      colabs.forEach(doc => {
        const d = doc.data();
        if (d.fcmToken) tokens.push(d.fcmToken);

        const notifRef = admin.firestore().collection('notifications').doc();
        batch.set(notifRef, {
          recipientId: doc.id,
          title: "Novo Cabaz Pedido",
          body: `${nomeAluno} pediu um cabaz.`,
          date: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
          type: "pedido_novo",
          relatedId: orderId,
          targetProfile: "COLABORADOR"
        });
      });

      await batch.commit();

      if (tokens.length > 0) {
        await admin.messaging().sendEachForMulticast({
          notification: { 
            title: "Novo Cabaz Pedido", 
            body: `${nomeAluno} pediu um cabaz.` 
          },
          tokens: tokens
        });
      }
      return null;
    } catch (e) { 
      console.error("Erro novo pedido:", e);
      return null; 
    }
  });

// ==================================================================
// 4. ATUALIZAÇÃO PEDIDO (Ação do COLABORADOR -> Avisa BENEFICIARIO)
// ==================================================================
exports.notificarAtualizacaoPedido = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    
    const novos = change.after.data();
    const antigos = change.before.data();
    const userId = novos.userId;
    
    if (!userId) return null;

    let titulo = "";
    let corpo = "";
    
    // REJEITADO
    if (antigos.accept !== "REJEITADA" && novos.accept === "REJEITADA") {
      titulo = "Pedido Recusado";
      corpo = "O seu pedido de cabaz foi recusado.";
    } 
    // ACEITE / AGENDADO
    else if (novos.accept === "ACEITE" && (antigos.accept !== "ACEITE" || novos.surveyDate !== antigos.surveyDate)) {
      let dataStr = "brevemente";
      if (novos.surveyDate) {
         const d = novos.surveyDate.toDate ? novos.surveyDate.toDate() : new Date(novos.surveyDate);
         dataStr = d.toLocaleDateString('pt-PT', { day: '2-digit', month: '2-digit', year: 'numeric' });
      }
      titulo = "Levantamento Agendado";
      corpo = `Levante o cabaz em: ${dataStr}`;
    }

    if (!titulo) return null;

    try {
      const uDoc = await admin.firestore().collection('users').doc(userId).get();
      const token = uDoc.exists ? uDoc.data().fcmToken : null;

      // Avisar Beneficiário
      await admin.firestore().collection('notifications').add({
        recipientId: userId,
        title: titulo,
        body: corpo,
        date: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
        type: "pedido_estado",
        relatedId: context.params.orderId,
        targetProfile: "BENEFICIARIO"
      });

      if (token) {
        await admin.messaging().send({
          notification: { title: titulo, body: corpo },
          token: token
        });
      }
      return null;
    } catch (e) { 
      console.error("Erro update pedido:", e);
      return null; 
    }
  });

// ==================================================================
// 5. VALIDADES (Cron Job -> Avisa COLABORADOR)
// ==================================================================
exports.verificarValidades = functions.pubsub
  .schedule('every 24 hours')
  .timeZone('Europe/Lisbon')
  .onRun(async (context) => {
    
    const hoje = new Date();
    const diasAlerta = 7; 
    const dataLimite = new Date();
    dataLimite.setDate(hoje.getDate() + diasAlerta);

    try {
      // Nota: Verifica se a coleção é 'product' ou 'products' na tua BD
      const snapshot = await admin.firestore().collection('product').get();
      let produtosAExpirar = [];

      snapshot.forEach(doc => {
        const produto = doc.data();
        const lotes = produto.batches || [];

        lotes.forEach(lote => {
          if (lote.validity) {
            const dataValidade = lote.validity.toDate ? lote.validity.toDate() : new Date(lote.validity);
            
            if (dataValidade > hoje && dataValidade <= dataLimite) {
              produtosAExpirar.push(`${produto.name} (Qtd: ${lote.quantity})`);
            }
          }
        });
      });

      if (produtosAExpirar.length === 0) return null;

      const listaResumida = produtosAExpirar.slice(0, 3).join(", ");
      const maisOutros = produtosAExpirar.length > 3 ? ` e mais ${produtosAExpirar.length - 3}` : "";
      const corpoMsg = `Atencao! Produtos a expirar em breve: ${listaResumida}${maisOutros}.`;

      const colabs = await admin.firestore().collection('users').where('isCollaborator', '==', true).get();
      const tokens = [];
      const batch = admin.firestore().batch();

      colabs.forEach(doc => {
        const d = doc.data();
        if (d.fcmToken) tokens.push(d.fcmToken);

        const ref = admin.firestore().collection('notifications').doc();
        batch.set(ref, {
          recipientId: doc.id,
          title: "Alerta de Validade",
          body: corpoMsg,
          date: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
          type: "validade_alerta",
          targetProfile: "COLABORADOR"
        });
      });

      await batch.commit();

      if (tokens.length > 0) {
        await admin.messaging().sendEachForMulticast({
          notification: {
            title: "Alerta de Validade",
            body: corpoMsg
          },
          tokens: tokens
        });
      }
      return null;

    } catch (erro) {
      console.error("Erro validades:", erro);
      return null;
    }
  });