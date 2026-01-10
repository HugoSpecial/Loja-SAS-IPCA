const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const PDFDocument = require('pdfkit');

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
    hoje.setHours(0, 0, 0, 0); 

    try {
      const snapshot = await admin.firestore().collection('product').get(); // Confirma se é 'product' ou 'products'
      
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

            const diffTempo = dataValidade.getTime() - hoje.getTime();
            const diffDias = Math.ceil(diffTempo / (1000 * 60 * 60 * 24));

            if (diffDias === 7) criticos7Dias.push(`${produto.name}`);
            else if (diffDias === 15) aviso15Dias.push(`${produto.name}`);
            else if (diffDias === 30) aviso30Dias.push(`${produto.name}`);
          }
        });
      });

      if (criticos7Dias.length === 0 && aviso15Dias.length === 0 && aviso30Dias.length === 0) {
        console.log("Nenhum produto a expirar nas datas alvo.");
        return null;
      }

      let titulo = "Alerta de Validade";
      let corpoMsg = "";

      if (aviso30Dias.length > 0) corpoMsg += `Atenção: ${aviso30Dias.slice(0, 3).join(", ")} expiram em 30 dias. `;
      if (aviso15Dias.length > 0) corpoMsg += `Atenção: ${aviso15Dias.slice(0, 3).join(", ")} expiram em 15 dias. `;
      if (criticos7Dias.length > 0) corpoMsg += `CRÍTICO: ${criticos7Dias.slice(0, 3).join(", ")} expiram em 1 semana!`;

      if (!corpoMsg || corpoMsg.trim() === "") return null;

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

  const novaJustificacao = dadosNovos.reason && dadosNovos.reason !== dadosAntigos.reason;
  if (!novaJustificacao) return null;

  let nomeAluno = "Um beneficiário";
  if (dadosNovos.userId) {
      const userDoc = await admin.firestore().collection('users').doc(dadosNovos.userId).get();
      if (userDoc.exists) {
          nomeAluno = userDoc.data().name || "Aluno";
      }
  }

  try {
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
        type: "resposta_entrega_rejeitada",
        relatedId: deliveryId,
        senderId: dadosNovos.userId || "",
        targetProfile: "COLABORADOR"
      });
    });

    await batch.commit();

    if (tokens.length > 0) {
      await admin.messaging().sendEachForMulticast({
        notification: { title: "Justificação de Falta", body: `${nomeAluno} enviou uma justificação.` },
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
    
    if (dadosNovos.reason === dadosAntigos.reason) return null;
    if (!dadosNovos.userId) return null;

    let titulo = "";
    let corpo = "";

    if (dadosNovos.reason.includes("Justificado")) {
      titulo = "Justificação Aceite";
      corpo = "A sua falta foi justificada com sucesso.";
    } else if (dadosNovos.reason.includes("Rejeitada") || dadosNovos.reason.includes("Falta Aplicada")) {
      titulo = "Justificação Recusada";
      corpo = "A justificação não foi aceite. Foi registada uma falta.";
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
        type: "pedido_estado",
        relatedId: context.params.deliveryId,
        targetProfile: "BENEFICIARIO"
      });

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
// 9. FECHO MENSAL COMPLETO (Pedidos, Entregas, Stock)
// ==================================================================
exports.backupMensalCompleto = functions
  .region('europe-west1')
  .pubsub.schedule('5 0 1 * *') // Dia 1 de cada mês às 00:05
  .timeZone('Europe/Lisbon')
  .onRun(async (context) => {

    const bucket = admin.storage().bucket();

    // 1. Calcular Mês Anterior (O mês que vamos fechar)
    const dataRef = new Date();
    dataRef.setMonth(dataRef.getMonth() - 1);
    
    const mesAlvo = dataRef.getMonth() + 1; 
    const anoAlvo = dataRef.getFullYear();

    console.log(`>>> A iniciar Fecho Mensal para ${mesAlvo}/${anoAlvo}`);

    // PASSO 1: LIMPEZA TOTAL (Apaga todos os relatórios desse mês, manuais e autos)
    const reportsAntigos = await admin.firestore().collection('reports')
      .where('month', '==', mesAlvo)
      .where('year', '==', anoAlvo)
      .get();

    if (!reportsAntigos.empty) {
      const batchDelete = admin.firestore().batch();
      reportsAntigos.forEach(doc => {
        batchDelete.delete(doc.ref);
      });
      await batchDelete.commit();
      console.log("Limpeza de relatórios antigos concluída.");
    }

    const startDate = new Date(anoAlvo, mesAlvo - 1, 1);
    const endDate = new Date(anoAlvo, mesAlvo, 0, 23, 59, 59);

    // ==============================================================
    // PASSO 2: PEDIDOS (ORDERS)
    // ==============================================================
    const ordersSnap = await admin.firestore().collection('orders')
      .where('orderDate', '>=', startDate)
      .where('orderDate', '<=', endDate)
      .orderBy('orderDate', 'desc')
      .get();

    if (!ordersSnap.empty) {
        await gerarPdfEstiloAndroid(bucket, 'orders', ordersSnap, mesAlvo, anoAlvo);
    }

    // ==============================================================
    // PASSO 3: ENTREGAS (DELIVERIES)
    // ==============================================================
    const deliverySnap = await admin.firestore().collection('delivery')
      .where('surveyDate', '>=', startDate)
      .where('surveyDate', '<=', endDate)
      .orderBy('surveyDate', 'desc')
      .get();

    if (!deliverySnap.empty) {
        await gerarPdfEstiloAndroid(bucket, 'delivery', deliverySnap, mesAlvo, anoAlvo);
    }

    // ==============================================================
    // PASSO 4: STOCK (PRODUCT)
    // ==============================================================
    const productSnap = await admin.firestore().collection('product').get(); // Ou 'products' dependendo da tua BD

    if (!productSnap.empty) {
        await gerarPdfEstiloAndroid(bucket, 'stock', productSnap, mesAlvo, anoAlvo);
    }

    console.log("Fecho mensal completo.");
    return null;
  });


// ==================================================================
// FUNÇÃO MESTRA DE DESENHO (Estilo Android - Canvas)
// ==================================================================
async function gerarPdfEstiloAndroid(bucket, tipoRelatorio, snapshot, mes, ano) {
    
    // Configurações Globais
    const MARGIN = 40;
    const PAGE_WIDTH = 595; 
    const PAGE_HEIGHT = 842;

    const doc = new PDFDocument({ margin: MARGIN, size: 'A4' });
    const chunks = [];
    
    doc.on('data', chunk => chunks.push(chunk));
    
    let pageNumber = 1;
    let y = 50; 

    // --- FUNÇÕES INTERNAS DE DESENHO ---
    
    const desenharCabecalho = () => {
        y = 50;
        const titulo = getTitulo(tipoRelatorio, mes, ano);
        
        doc.fillColor('black').font('Helvetica-Bold').fontSize(20).text(titulo, MARGIN, y);
        y += 30;
        doc.font('Helvetica').fontSize(14).text(`Página ${pageNumber} (Total: ${snapshot.size})`, MARGIN, y);
        y += 40;

        // Cabeçalhos da Tabela
        doc.font('Helvetica-Bold').fontSize(11);
        const cols = getColunas(tipoRelatorio);
        
        cols.forEach(col => {
            doc.text(col.text, col.x, y);
        });

        y += 15;
        doc.moveTo(MARGIN, y).lineTo(PAGE_WIDTH - MARGIN, y).stroke();
        y += 20;
    };

    // Desenha o cabeçalho da primeira página
    desenharCabecalho();

    // Iterar dados
    snapshot.forEach(docSnap => {
        const data = docSnap.data();

        // Verificar se precisa de nova página
        if (y > PAGE_HEIGHT - 60) {
            doc.addPage();
            pageNumber++;
            desenharCabecalho();
        }

        doc.font('Helvetica').fontSize(10).fillColor('black');

        // DESENHAR LINHAS CONSOANTE O TIPO
        if (tipoRelatorio === 'orders') {
            const dataStr = data.orderDate ? data.orderDate.toDate().toLocaleDateString('pt-PT') : '--';
            const nome = (data.userName || 'Anónimo').substring(0, 25);
            
            // Estado e Colaborador
            let estadoTexto = data.accept || 'PENDENTE';
            let detalheAvaliacao = "";
            if (estadoTexto === 'ACEITE' || estadoTexto === 'REJEITADA') {
                 const colab = data.evaluatedBy || "Colaborador";
                 const dataEval = data.evaluationDate ? data.evaluationDate.toDate().toLocaleDateString('pt-PT') : "";
                 detalheAvaliacao = `Por: ${colab} (${dataEval})`;
            }

            // Itens
            let itemsStr = "Sem itens";
            if (data.items && data.items.length) {
                itemsStr = data.items.map(i => `${i.name} (${i.quantity})`).join(', ');
            }
            
            // X: 40, 110, 300
            doc.text(dataStr, 40, y);
            doc.font('Helvetica-Bold').text(nome, 110, y);
            
            doc.font('Helvetica').fontSize(9).fillColor('#555');
            doc.text(estadoTexto, 110, y + 12);
            if(detalheAvaliacao) doc.text(detalheAvaliacao, 110, y + 22);

            // Itens
            doc.fillColor('black').fontSize(10);
            doc.text(itemsStr, 300, y, { width: 250, align: 'left' });
            
            const height = doc.heightOfString(itemsStr, { width: 250 });
            y += Math.max(40, height + 10);

        } else if (tipoRelatorio === 'delivery') {
            const dataStr = data.surveyDate ? data.surveyDate.toDate().toLocaleDateString('pt-PT') : '--';
            const nomeBenef = (data.userName || 'Anónimo').substring(0, 25);
            const estado = data.state || 'PENDENTE';
            const colab = data.evaluatedBy || "--";

            // X: 40, 130, 330, 430
            doc.text(dataStr, 40, y);
            doc.font('Helvetica-Bold').text(nomeBenef, 130, y);
            
            doc.font('Helvetica').text(estado, 330, y); 

            doc.text(colab, 430, y);
            if (data.evaluationDate) {
                const evalDate = data.evaluationDate.toDate().toLocaleDateString('pt-PT');
                doc.fontSize(8).fillColor('#555').text(evalDate, 430, y + 10);
            }
            y += 30;

        } else if (tipoRelatorio === 'stock') {
            const nomeProd = (data.name || 'Sem nome').substring(0, 30);
            const categoria = data.category || '--';
            
            let validQty = 0;
            let expiredQty = 0;
            const now = new Date();
            
            if (data.batches) {
                data.batches.forEach(b => {
                    const validade = b.validity ? b.validity.toDate() : null;
                    if (validade && validade >= now) validQty += (b.quantity || 0);
                    else expiredQty += (b.quantity || 0);
                });
            }

            // X: 40, 250, 380, 480
            doc.text(nomeProd, 40, y);
            doc.text(categoria, 250, y);
            doc.text(`${validQty} un`, 380, y);
            
            if (expiredQty > 0) doc.fillColor('red').font('Helvetica-Bold');
            else doc.fillColor('#ccc');
            
            doc.text(`${expiredQty} un`, 480, y);
            
            y += 25;
        }
    });

    doc.end();

    return new Promise((resolve, reject) => {
        doc.on('end', async () => {
            try {
                const buffer = Buffer.concat(chunks);
                const dbName = getDbName(tipoRelatorio);
                const fileName = `AUTO_${dbName}_${ano}_${mes}_${Date.now()}.pdf`;
                const file = bucket.file(`reports/${fileName}`);

                await file.save(buffer, { metadata: { contentType: 'application/pdf' } });
                
                const [url] = await file.getSignedUrl({ 
                    action: 'read', 
                    expires: Date.now() + 1000 * 60 * 60 * 24 * 365 * 10 
                });

                await admin.firestore().collection('reports').add({
                    title: getTitulo(tipoRelatorio, mes, ano),
                    month: mes,
                    year: ano,
                    totalOrders: snapshot.size, // Reaproveita o campo totalOrders para qtd
                    generatedAt: admin.firestore.FieldValue.serverTimestamp(),
                    generatedBy: 'SISTEMA (AUTO)',
                    type: dbName, 
                    fileUrl: url,
                    storagePath: `reports/${fileName}`
                });
                console.log(`PDF ${tipoRelatorio} gerado com sucesso.`);
                resolve();
            } catch (e) {
                console.error(e);
                reject(e);
            }
        });
    });
}

// Helpers
function getTitulo(tipo, mes, ano) {
    if (tipo === 'orders') return `Relatório Mensal - ${mes}/${ano}`;
    if (tipo === 'delivery') return `Relatório de Entregas - ${mes}/${ano}`;
    if (tipo === 'stock') return `Inventário de Stock - ${mes}/${ano}`;
    return 'Relatório';
}

function getDbName(tipo) {
    if (tipo === 'orders') return 'auto_backup'; // Mantém compatibilidade com o teu histórico
    if (tipo === 'delivery') return 'delivery_report';
    if (tipo === 'stock') return 'stock_report';
    return 'unknown';
}

function getColunas(tipo) {
    if (tipo === 'orders') return [
        { text: 'Data', x: 40 }, { text: 'Beneficiário / Avaliação', x: 110 }, { text: 'Itens', x: 300 }
    ];
    if (tipo === 'delivery') return [
        { text: 'Data', x: 40 }, { text: 'Beneficiário', x: 130 }, { text: 'Estado', x: 330 }, { text: 'Avaliado Por', x: 430 }
    ];
    if (tipo === 'stock') return [
        { text: 'Produto', x: 40 }, { text: 'Categoria', x: 250 }, { text: 'Qtd. Válida', x: 380 }, { text: 'Qtd. Expirada', x: 480 }
    ];
    return [];
}