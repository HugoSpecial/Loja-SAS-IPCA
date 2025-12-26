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
// FUNÇÃO 2: AVISA O ALUNO QUANDO O ESTADO DA CANDIDATURA MUDA (onUpdate)
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

// ==================================================================
// 3. ENCOMENDAS (CABAZES) - NOVO PEDIDO (onCreate)
// Avisa os Colaboradores que um aluno pediu um cabaz
// ==================================================================
exports.notificarNovoPedido = functions.firestore
.document('orders/{orderId}')
.onCreate(async (snap, context) => {
  
  const dados = snap.data();
  const orderId = context.params.orderId;
  // Tenta apanhar o nome do user que vem na encomenda, ou usa genérico
  const nomeAluno = dados.userName || "Um beneficiário";

  try {
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

      // Histórico
      const notifRef = admin.firestore().collection('notifications').doc();
      batch.set(notifRef, {
        recipientId: doc.id,
        title: "Novo Pedido de Cabaz",
        body: `${nomeAluno} fez um novo pedido de cabaz.`,
        date: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
        type: "pedido_novo",
        relatedId: orderId,
        forCollaborator: true
      });
    });

    await batch.commit();

    if (tokens.length > 0) {
      await admin.messaging().sendEachForMulticast({
        notification: {
          title: "Novo Pedido de Cabaz",
          body: `${nomeAluno} fez um novo pedido de cabaz.`
        },
        tokens: tokens
      });
    }
    return null;
  } catch (error) {
    console.error("Erro novo pedido:", error);
    return null;
  }
});

// ==================================================================
// 4. ENCOMENDAS - ATUALIZAÇÃO DE ESTADO/DATA (onUpdate)
// Gere Aceitação, Rejeição e Agendamento de Levantamento
// ==================================================================
exports.notificarAtualizacaoPedido = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    
    const dadosNovos = change.after.data();
    const dadosAntigos = change.before.data();
    const orderId = context.params.orderId;
    
    const novoEstado = dadosNovos.accept;     
    const novaData = dadosNovos.surveyDate;   
    const userId = dadosNovos.userId;

    if (!userId) return null;

    const userDoc = await admin.firestore().collection('users').doc(userId).get();
    const userToken = userDoc.exists ? userDoc.data().fcmToken : null;

    const batch = admin.firestore().batch();
    
    let titulo = "";
    let corpo = "";
    let enviarParaAluno = false;
    let enviarParaColabs = false;

    // A: REJEITADO
    if (dadosAntigos.accept !== "REJEITADA" && novoEstado === "REJEITADA") {
      titulo = "Pedido de Cabaz Rejeitado";
      corpo = "O seu pedido de cabaz não pôde ser aceite. Verifique o motivo na app.";
      enviarParaAluno = true;
    }

    // B: ACEITE / DATA DEFINIDA
    else if (novoEstado === "ACEITE" && (dadosAntigos.accept !== "ACEITE" || novaData !== dadosAntigos.surveyDate)) {
      
      let dataLegivel = "brevemente";
      if (novaData) {
        const dateObj = novaData.toDate ? novaData.toDate() : new Date(novaData);
        // CORREÇÃO: Apenas Dia, Mês e Ano
        dataLegivel = dateObj.toLocaleDateString('pt-PT', { 
          day: '2-digit', 
          month: '2-digit',
          year: 'numeric' 
        });
      }

      titulo = "Levantamento Agendado";
      corpo = `O seu cabaz foi aceite! Deve ser levantado em: ${dataLegivel}.`;
      enviarParaAluno = true;
      enviarParaColabs = true; 
    }

    if (!titulo) return null;

    try {
      // 1. Notificar ALUNO
      if (enviarParaAluno) {
        const refAluno = admin.firestore().collection('notifications').doc();
        batch.set(refAluno, {
          recipientId: userId,
          title: titulo,
          body: corpo,
          date: admin.firestore.FieldValue.serverTimestamp(),
          read: false,
          type: "pedido_estado",
          relatedId: orderId,
          forCollaborator: false
        });

        if (userToken) {
          await admin.messaging().send({
            notification: { title: titulo, body: corpo },
            token: userToken
          });
        }
      }

      // 2. Notificar COLABORADORES
      if (enviarParaColabs) {
        const colabsSnapshot = await admin.firestore().collection('users').where('isCollaborator', '==', true).get();
        const colabTokens = [];

        colabsSnapshot.forEach(doc => {
          const cData = doc.data();
          if (cData.fcmToken) colabTokens.push(cData.fcmToken);

          const refColab = admin.firestore().collection('notifications').doc();
          batch.set(refColab, {
            recipientId: doc.id,
            title: "Agendamento de Entrega",
            body: `Cabaz para ${dadosNovos.userName || "aluno"} agendado para ${novaData ? dataLegivel : "brevemente"}.`,
            date: admin.firestore.FieldValue.serverTimestamp(),
            read: false,
            type: "pedido_agendado",
            relatedId: orderId,
            forCollaborator: true
          });
        });

        if (colabTokens.length > 0) {
          await admin.messaging().sendEachForMulticast({
            notification: { 
              title: "Agendamento de Entrega", 
              body: `Cabaz pronto para entrega a ${dadosNovos.userName}.` 
            },
            tokens: colabTokens
          });
        }
      }

      await batch.commit();
      return null;

    } catch (erro) {
      console.error("Erro update pedido:", erro);
      return null;
    }
  });

// ==================================================================
// 5. VERIFICAR VALIDADES (Agendado / Cron Job)
// Corre todos os dias às 09:00 da manhã
// ==================================================================
exports.verificarValidades = functions.pubsub
.schedule('every 24 hours') // Podes mudar para 'every day 09:00'
.timeZone('Europe/Lisbon')
.onRun(async (context) => {
  
  const hoje = new Date();
  // Definir alerta para produtos que acabam nos próximos 7 dias
  const diasAlerta = 7; 
  const dataLimite = new Date();
  dataLimite.setDate(hoje.getDate() + diasAlerta);

  try {
    // Buscar todos os produtos
    const snapshot = await admin.firestore().collection('products').get();
    
    let produtosAExpirar = [];

    snapshot.forEach(doc => {
      const produto = doc.data();
      const lotes = produto.batches || []; // Array de ProductBatch

      lotes.forEach(lote => {
        if (lote.validity) {
          // Converter Timestamp para Date
          const dataValidade = lote.validity.toDate ? lote.validity.toDate() : new Date(lote.validity);
          
          // Verifica se a validade é no futuro mas antes do limite (ex: próx 7 dias)
          if (dataValidade > hoje && dataValidade <= dataLimite) {
            produtosAExpirar.push(`${produto.name} (Qtd: ${lote.quantity})`);
          }
        }
      });
    });

    // Se não houver nada a estragar-se, não faz nada
    if (produtosAExpirar.length === 0) {
      console.log("Nenhum produto próximo da validade.");
      return null;
    }

    // Criar a mensagem (junta os 3 primeiros nomes para não ficar gigante)
    const listaResumida = produtosAExpirar.slice(0, 3).join(", ");
    const maisOutros = produtosAExpirar.length > 3 ? ` e mais ${produtosAExpirar.length - 3}` : "";
    
    const corpoMsg = `Atenção! Produtos a expirar em breve: ${listaResumida}${maisOutros}.`;

    // Notificar Colaboradores
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
        forCollaborator: true
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

    console.log("Alerta de validades enviado.");
    return null;

  } catch (erro) {
    console.error("Erro validades:", erro);
    return null;
  }
});