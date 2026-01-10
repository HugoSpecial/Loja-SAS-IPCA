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
// 5. VALIDADES (Cron Job -> Avisa COLABORADOR aos 30, 15 e 7 dias)
// ==================================================================
exports.verificarValidades = functions.pubsub
  .schedule('every 24 hours')
  .timeZone('Europe/Lisbon')
  .onRun(async (context) => {
    
    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0); // Normaliza para comparar apenas datas

    try {
      const snapshot = await admin.firestore().collection('product').get();
      
      let criticos7Dias = [];
      let aviso15Dias = [];
      let aviso30Dias = [];

      snapshot.forEach(doc => {
        const produto = doc.data();
        const lotes = produto.batches || [];

        lotes.forEach(lote => {
          if (lote.validity) {
            const dataValidade = lote.validity.toDate ? lote.validity.toDate() : new Date(lote.validity);
            dataValidade.setHours(0, 0, 0, 0);

            // Cálculo da diferença em dias
            const diffTempo = dataValidade.getTime() - hoje.getTime();
            const diffDias = Math.ceil(diffTempo / (1000 * 60 * 60 * 24));

            // Verifica os dias exatos
            if (diffDias === 7) {
              criticos7Dias.push(`${produto.name}`);
            } else if (diffDias === 15) {
              aviso15Dias.push(`${produto.name}`);
            } else if (diffDias === 30) {
              aviso30Dias.push(`${produto.name}`);
            }
          }
        });
      });

      if (criticos7Dias.length === 0 && aviso15Dias.length === 0 && aviso30Dias.length === 0) {
        console.log("Nenhum produto a expirar nas datas alvo (7, 15 ou 30 dias).");
        return null;
      }

      // Montagem da mensagem
      let titulo = "Alerta de Validade";
      let corpoMsg = "";

      if (aviso30Dias.length > 0) {
        corpoMsg += `Atenção: ${aviso30Dias.slice(0, 3).join(", ")} expiram em 30 dias. `;
      }
      if (aviso15Dias.length > 0) {
        corpoMsg += `Atenção: ${aviso15Dias.slice(0, 3).join(", ")} expiram em 15 dias. `;
      }
      if (criticos7Dias.length > 0) {
        corpoMsg += `CRÍTICO: ${criticos7Dias.slice(0, 3).join(", ")} expiram em 1 semana!`;
      }

      // SEGURANÇA EXTRA: Se a mensagem estiver vazia, não envia nada
      if (!corpoMsg || corpoMsg.trim() === "") {
         return null;
      }

      // Buscar tokens dos colaboradores
      const colabs = await admin.firestore().collection('users').where('isCollaborator', '==', true).get();
      
      if (colabs.empty) return null;

      const tokens = [];
      const batch = admin.firestore().batch();

      colabs.forEach(doc => {
        const d = doc.data();
        if (d.fcmToken) tokens.push(d.fcmToken);

        const ref = admin.firestore().collection('notifications').doc();
        batch.set(ref, {
          recipientId: doc.id,
          title: titulo,
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
          notification: { title: titulo, body: corpoMsg },
          tokens: tokens
        });
      }

      console.log("Notificação de validade enviada com sucesso.");
      return null;
    } catch (erro) {
      console.error("Erro no cron de validades:", erro);
      return null;
    }
  });

  // ==================================================================
// 6. NOVA JUSTIFICAÇÃO (Ação do BENEFICIÁRIO -> Avisa COLABORADOR)
// ==================================================================
exports.notificarJustificacao = functions.firestore
.document('delivery/{deliveryId}')
.onUpdate(async (change, context) => {

  const dadosNovos = change.after.data();
  const dadosAntigos = change.before.data();
  const deliveryId = context.params.deliveryId;

  // Lógica: Se o campo 'reason' foi alterado e agora tem texto,
  // e antes estava vazio (ou o estado mudou para algo que indique justificação)
  // Ajusta esta condição conforme a tua lógica de app (ex: state == "JUSTIFICACAO_PENDENTE")
  const novaJustificacao = dadosNovos.reason && dadosNovos.reason !== dadosAntigos.reason;
  
  // Se não for uma justificação nova, ignora
  if (!novaJustificacao) return null;

  // Buscar dados do aluno para meter o nome na notificação
  let nomeAluno = "Um beneficiário";
  if (dadosNovos.userId) {
      const userDoc = await admin.firestore().collection('users').doc(dadosNovos.userId).get();
      if (userDoc.exists) {
          nomeAluno = userDoc.data().name || "Aluno";
      }
  }

  try {
    // Buscar Colaboradores
    const colabs = await admin.firestore().collection('users').where('isCollaborator', '==', true).get();
    if (colabs.empty) return null;

    const batch = admin.firestore().batch();
    const tokens = [];

    colabs.forEach(doc => {
      const adminData = doc.data();
      if (adminData.fcmToken) tokens.push(adminData.fcmToken);

      const notifRef = admin.firestore().collection('notifications').doc();
      batch.set(notifRef, {
        recipientId: doc.id,
        title: "Justificação de Falta",
        body: `${nomeAluno} enviou uma justificação.`,
        date: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
        type: "resposta_entrega_rejeitada", // O TIPO QUE A APP ESPERA
        relatedId: deliveryId,
        senderId: dadosNovos.userId || "", // Importante para o popup saber quem é
        targetProfile: "COLABORADOR"
      });
    });

    await batch.commit();

    if (tokens.length > 0) {
      await admin.messaging().sendEachForMulticast({
        notification: { 
          title: "Justificação de Falta", 
          body: `${nomeAluno} enviou uma justificação.` 
        },
        tokens: tokens
      });
    }
    return null;
  } catch (error) { 
    console.error("Erro notificação justificação:", error);
    return null; 
  }
});

// ==================================================================
// 7. DECISÃO JUSTIFICATIVA (Ação do COLABORADOR -> Avisa BENEFICIÁRIO)
// ==================================================================
exports.notificarDecisaoJustificacao = functions.firestore
  .document('delivery/{deliveryId}')
  .onUpdate(async (change, context) => {

    const dadosNovos = change.after.data();
    const dadosAntigos = change.before.data();
    
    // Verifica se a razão mudou (sinal que o colaborador tomou uma decisão)
    // E garante que há um user associado para receber a notificação
    if (dadosNovos.reason === dadosAntigos.reason) return null;
    if (!dadosNovos.userId) return null;

    let titulo = "";
    let corpo = "";

    // Lógica baseada nas strings que usas no Android ViewModel
    if (dadosNovos.reason.includes("Justificado")) {
      titulo = "Justificação Aceite";
      corpo = "A sua falta foi justificada com sucesso.";
    } else if (dadosNovos.reason.includes("Rejeitada") || dadosNovos.reason.includes("Falta Aplicada")) {
      titulo = "Justificação Recusada";
      corpo = "A justificação não foi aceite. Foi registada uma falta.";
    } else {
      // Se for apenas o aluno a submeter a justificação inicial, ignoramos aqui 
      // (porque isso é tratado na função 'notificarJustificacao')
      return null;
    }

    try {
      // Buscar Token do Aluno
      const userDoc = await admin.firestore().collection('users').doc(dadosNovos.userId).get();
      const userToken = userDoc.exists ? userDoc.data().fcmToken : null;

      // Gravar no histórico do Aluno
      await admin.firestore().collection('notifications').add({
        recipientId: dadosNovos.userId,
        title: titulo,
        body: corpo,
        date: admin.firestore.FieldValue.serverTimestamp(),
        read: false,
        type: "pedido_estado", // Reutilizamos este tipo ou crias um novo "justificacao_decisao"
        relatedId: context.params.deliveryId,
        targetProfile: "BENEFICIARIO"
      });

      // Enviar Push
      if (userToken) {
        await admin.messaging().send({
          notification: { title: titulo, body: corpo },
          token: userToken
        });
      }
      return null;
    } catch (error) { 
      console.error("Erro decisão justificação:", error);
      return null; 
    }
  });

  // ==================================================================
// 9. BACKUP MENSAL AUTOMÁTICO (Dia 1 de cada mês às 00:05)
// ==================================================================
exports.backupRelatorioMensal = functions
.region('europe-west1')
.pubsub.schedule('5 0 1 * *') // Executa: Minuto 5, Hora 0, Dia 1
.timeZone('Europe/Lisbon')
.onRun(async (context) => {
    
    // Calcular o mês anterior (o mês que acabou de fechar)
    const dataRef = new Date();
    dataRef.setMonth(dataRef.getMonth() - 1);
    
    const mesAlvo = dataRef.getMonth() + 1; // 1 a 12
    const anoAlvo = dataRef.getFullYear();

    console.log(`>>> A iniciar Backup Automático para ${mesAlvo}/${anoAlvo}`);

    // 1. Apagar relatórios antigos desse mês (Evita duplicados do Manual ou Auto anterior)
    const reportsAntigos = await admin.firestore().collection('reports')
        .where('month', '==', mesAlvo)
        .where('year', '==', anoAlvo)
        .get();

    if (!reportsAntigos.empty) {
        const batchDelete = admin.firestore().batch();
        reportsAntigos.forEach(doc => {
            console.log(`Apagando relatório antigo: ${doc.id}`);
            batchDelete.delete(doc.ref);
        });
        await batchDelete.commit();
    }

    // 2. Buscar os dados do Firestore
    const startDate = new Date(anoAlvo, mesAlvo - 1, 1);
    const endDate = new Date(anoAlvo, mesAlvo, 0, 23, 59, 59);

    const snapshot = await admin.firestore().collection('orders')
        .where('orderDate', '>=', startDate)
        .where('orderDate', '<=', endDate)
        .orderBy('orderDate', 'desc')
        .get();

    if (snapshot.empty) {
        console.log("Sem dados para gerar relatório.");
        return null;
    }

    // 3. Preparar Tabela PDF
    const tableBody = [
        [
            { text: 'Data', style: 'tableHeader' }, 
            { text: 'Beneficiário', style: 'tableHeader' }, 
            { text: 'Avaliado Por', style: 'tableHeader' },
            { text: 'Itens', style: 'tableHeader' }
        ]
    ];

    snapshot.forEach(doc => {
        const o = doc.data();
        const dataStr = o.orderDate ? o.orderDate.toDate().toLocaleDateString('pt-PT') : '--';
        
        // Formatar Itens
        let itensStr = "";
        if(o.items && o.items.length) {
            itensStr = o.items.map(i => `${i.name} (${i.quantity})`).join(', ');
        }

        // Formatar Avaliador (usando os teus campos novos)
        let avaliadorStr = "--";
        if (o.accept === "ACEITE" || o.accept === "REJEITADA") {
            avaliadorStr = o.evaluatedBy || "Colaborador";
            if(o.evaluationDate) {
                const dataEval = o.evaluationDate.toDate().toLocaleDateString('pt-PT');
                avaliadorStr += ` (${dataEval})`;
            }
        }

        tableBody.push([
            { text: dataStr, style: 'rowStyle' },
            { text: o.userName || 'Anónimo', style: 'rowStyle' },
            { text: avaliadorStr, style: 'rowStyle' },
            { text: itensStr, style: 'rowStyle' }
        ]);
    });

    const printer = new PdfPrinter({
        Roboto: {
            normal: 'Helvetica',
            bold: 'Helvetica-Bold',
            italics: 'Helvetica-Oblique',
            bolditalics: 'Helvetica-BoldOblique'
        }
    });

    const docDefinition = {
        content: [
            { text: `Relatório Mensal (Backup) - ${mesAlvo}/${anoAlvo}`, style: 'header' },
            { text: `Total de Pedidos: ${snapshot.size}`, margin: [0, 0, 0, 10] },
            {
                table: {
                    headerRows: 1,
                    widths: ['auto', 'auto', 'auto', '*'],
                    body: tableBody
                },
                layout: 'lightHorizontalLines'
            }
        ],
        styles: {
            header: { fontSize: 18, bold: true, margin: [0, 0, 0, 10] },
            tableHeader: { bold: true, fontSize: 10, fillColor: '#eeeeee' },
            rowStyle: { fontSize: 9 }
        },
        defaultStyle: { font: 'Roboto' }
    };

    // 4. Gerar PDF e Upload
    const pdfDoc = printer.createPdfKitDocument(docDefinition);
    const chunks = [];
    
    return new Promise((resolve, reject) => {
        pdfDoc.on('data', chunk => chunks.push(chunk));
        pdfDoc.on('end', async () => {
            const buffer = Buffer.concat(chunks);
            const fileName = `Backup_${anoAlvo}_${mesAlvo}_${Date.now()}.pdf`;
            const file = admin.storage().bucket().file(`reports/${fileName}`);

            await file.save(buffer, { metadata: { contentType: 'application/pdf' } });
            
            // URL válido por 10 anos
            const [url] = await file.getSignedUrl({ 
                action: 'read', 
                expires: Date.now() + 1000 * 60 * 60 * 24 * 365 * 10 
            });

            // 5. Gravar Registo na Coleção
            await admin.firestore().collection('reports').add({
                title: `Relatório Mensal - ${mesAlvo}/${anoAlvo}`,
                month: mesAlvo,
                year: anoAlvo,
                totalOrders: snapshot.size,
                generatedAt: admin.firestore.FieldValue.serverTimestamp(),
                generatedBy: 'SISTEMA (AUTO)',
                type: 'auto_backup',
                fileUrl: url,
                storagePath: `reports/${fileName}`
            });
            
            console.log("Relatório automático gerado com sucesso.");
            resolve();
        });
        pdfDoc.on('error', reject);
        pdfDoc.end();
    });
});