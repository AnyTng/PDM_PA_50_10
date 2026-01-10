import { onSchedule } from "firebase-functions/v2/scheduler";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp, FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { getAuth } from "firebase-admin/auth";

import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";

initializeApp();

export const deleteAuthUser = onCall(
  { region: "europe-southwest1" },
  async (request) => {
    const callerUid = request.auth?.uid;
    if (!callerUid) {
      throw new HttpsError("unauthenticated", "Utilizador nao autenticado.");
    }

    const targetUid = String(request.data?.uid ?? "").trim();
    if (!targetUid) {
      throw new HttpsError("invalid-argument", "UID em falta.");
    }

    const db = getFirestore();
    const adminSnap = await db
      .collection("funcionarios")
      .where("uid", "==", callerUid)
      .limit(1)
      .get();

    const role = adminSnap.docs[0]?.get("role") ?? "";
    if (!String(role).trim().toLowerCase().startsWith("admin")) {
      throw new HttpsError("permission-denied", "Sem permissoes.");
    }

    try {
      await getAuth().deleteUser(targetUid);
      return { ok: true };
    } catch (err: any) {
      const code = String(err?.code ?? "");
      if (code.includes("auth/user-not-found")) {
        return { ok: true, alreadyDeleted: true };
      }
      throw new HttpsError("internal", "Erro ao apagar autenticacao.");
    }
  }
);

export const notifyExpiringProducts7d = onSchedule(
  { schedule: "0 9 * * *", timeZone: "Europe/Lisbon" }, //como esta é tds os dias as 9 ("0 9 * * *") |para testar a cada minuto usar: "* * * * *"|
  async () => {
    const db = getFirestore();

    // Janela UTC: [D+7 00:00, D+8 00:00)
    const start = new Date();
    start.setUTCDate(start.getUTCDate() + 7);
    start.setUTCHours(0, 0, 0, 0);

    const end = new Date(start);
    end.setUTCDate(end.getUTCDate() + 1);

    const snap = await db
      .collection("produtos")
      .where("validade", ">=", Timestamp.fromDate(start))
      .where("validade", "<", Timestamp.fromDate(end))
      .where("alertaValidade7d", "!=", true)
      .get();

    if (snap.empty) return;

    // Só os "Disponivel"
    const docs = snap.docs.filter((d) => {
      const estado = String(d.get("estadoProduto") ?? "").trim().toLowerCase();
      return estado === "disponivel" || estado.startsWith("dispon");
    });

    if (docs.length === 0) return;

    const preview = docs
      .slice(0, 5)
      .map((d) => String(d.get("nomeProduto") ?? "Produto"))
      .join(", ");

    const body =
      docs.length <= 5 ? preview : `${preview} e mais ${docs.length - 5}…`;

    await getMessaging().send({
      topic: "funcionarios",
      notification: {
        title: "Produtos a expirar em 7 dias",
        body,
      },
      data: { type: "EXPIRY_7D", count: String(docs.length) },
    });

    // Marca como enviado
    const batch = db.batch();
    docs.forEach((doc) => {
      batch.update(doc.ref, {
        alertaValidade7d: true,
        alertaValidade7dEm: Timestamp.now(),
      });
    });
    await batch.commit();
  }
);

export const notifyApoiadoEstadoContaChanged = onDocumentUpdated(
  { document: "apoiados/{apoiadoId}", region: "europe-southwest1" },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    const beforeStatus = String(before.estadoConta ?? "");
    const afterStatus = String(after.estadoConta ?? "");
    if (beforeStatus === afterStatus) return;

    const apoiadoId = event.params.apoiadoId;
    const db = getFirestore();

    const tokensSnap = await db
      .collection("apoiados")
      .doc(apoiadoId)
      .collection("fcmTokens")
      .get();

    const tokens = tokensSnap.docs.map((d) => d.id).filter(Boolean);
    if (tokens.length === 0) return;

    const res = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "Estado da conta atualizado",
        body: `O estado da tua conta mudou para: ${afterStatus}`,
      },
      data: { type: "ACCOUNT_STATUS_CHANGED", estadoConta: afterStatus, apoiadoId },
    });

    // limpar tokens inválidos
    const batch = db.batch();
    res.responses.forEach((r, idx) => {
      if (!r.success) {
        const code = (r.error as any)?.code ?? "";
        if (code.includes("registration-token-not-registered") || code.includes("invalid-argument")) {
          batch.delete(tokensSnap.docs[idx].ref);
        }
      }
    });
    await batch.commit();
  }
);

export const notifyUrgentHelpRequest = onDocumentCreated(
  { document: "pedidos_ajuda/{pedidoId}", region: "europe-southwest1" },
  async (event) => {
    const pedidoId = event.params.pedidoId;
    const data = event.data?.data();
    if (!data) return;

    // Só dispara se for urgente
    const tipo = String(data.tipo ?? "").trim().toLowerCase();
    if (tipo !== "urgente") return;

    // Mensagem (usa campos que vocês já têm)
    const descricao = String(data.descricao ?? "Sem descrição");
    const apoiadoId = String(data.numeroMecanografico ?? ""); // no vosso exemplo é "a1"

    await getMessaging().send({
      topic: "funcionarios", // ✅ funcionários + admin
      notification: {
        title: "🚨 Pedido de Ajuda URGENTE",
        body: apoiadoId ? `(${apoiadoId}) ${descricao}` : descricao,
      },
      data: {
        type: "URGENT_HELP_REQUEST",
        pedidoId: String(pedidoId),
        apoiadoId: apoiadoId,
      },
    });
  }
);

export const notifyApoiadoPedidoDecision = onDocumentUpdated(
  { document: "pedidos_ajuda/{pedidoId}", region: "europe-southwest1" },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    // normalização: lower + trim
    const normalize = (v: any) => String(v ?? "").trim().toLowerCase();

    const beforeEstado = normalize(before.estado);
    const afterEstado = normalize(after.estado);

    // só quando muda
    if (beforeEstado === afterEstado) return;

    // ✅ estados “decisão final” (ajusta aqui se quiseres mais)
    const isAceite =
      afterEstado === "preparar_apoio" || afterEstado === "preparar apoio";

    const isNegado =
      afterEstado === "negado" || afterEstado === "rejeitado" || afterEstado === "recusado";

    if (!isAceite && !isNegado) return;

    const pedidoId = event.params.pedidoId;

    // no vosso pedido: numeroMecanografico = "a1" (id do doc em apoiados)
    const apoiadoId = String(after.numeroMecanografico ?? "").trim();
    if (!apoiadoId) return;

    const db = getFirestore();

    // tokens do apoiado
    const tokensSnap = await db
      .collection("apoiados")
      .doc(apoiadoId)
      .collection("fcmTokens")
      .get();

    const tokens = tokensSnap.docs.map((d) => d.id).filter(Boolean);
    if (tokens.length === 0) return;

    const descricao = String(after.descricao ?? "O teu pedido foi analisado");
    const body = descricao.length > 120 ? `${descricao.slice(0, 120)}…` : descricao;

    const title = isAceite
      ? "✅ Pedido de ajuda aceite"
      : "❌ Pedido de ajuda negado";

    const res = await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title, body },
      data: {
        type: "HELP_REQUEST_DECISION",
        pedidoId: String(pedidoId),
        estado: String(after.estado ?? ""),
      },
    });

    // limpar tokens inválidos
    const batch = db.batch();
    res.responses.forEach((r, idx) => {
      if (!r.success) {
        const code = (r.error as any)?.code ?? "";
        if (code.includes("registration-token-not-registered") || code.includes("invalid-argument")) {
          batch.delete(tokensSnap.docs[idx].ref);
        }
      }
    });
    await batch.commit();
  }
);


export const notifyCestaAgendadaReminders = onSchedule(
  { schedule: "0 9 * * *", timeZone: "Europe/Lisbon" }, //como esta é tds os dias as 9 ("0 9 * * *") |para testar a cada minuto usar: "* * * * *"|
  async () => {
    const db = getFirestore();
    const messaging = getMessaging();

    const DATE_FIELD = "dataAgendada";  // ✅ base
    const APOIADO_FIELD = "apoiadoID";  // ✅ "a2"
    const ESTADO_FIELD = "estadoCesta"; // ✅ "Agendada", "Entregue", "Nao_Levantou", etc.

    // normaliza texto: lowercase + remove espaços/underscores
    const normEstado = (v: any) =>
      String(v ?? "")
        .trim()
        .toLowerCase()
        .replace(/[\s_]+/g, ""); // "Nao_Levantou" -> "naolevantou"

    const mkWindowUTC = (daysAhead: number) => {
      const start = new Date();
      start.setUTCDate(start.getUTCDate() + daysAhead);
      start.setUTCHours(0, 0, 0, 0);

      const end = new Date(start);
      end.setUTCDate(end.getUTCDate() + 1);

      return { startTs: Timestamp.fromDate(start), endTs: Timestamp.fromDate(end) };
    };

    const configs = [
      { days: 7, flag: "notifRecolha7d", flagEm: "notifRecolha7dEm", label: "Daqui a 7 dias" },
      { days: 1, flag: "notifRecolha1d", flagEm: "notifRecolha1dEm", label: "Amanhã" },
      { days: 0, flag: "notifRecolha0d", flagEm: "notifRecolha0dEm", label: "Hoje" },
    ] as const;

    for (const { days, flag, flagEm, label } of configs) {
      const { startTs, endTs } = mkWindowUTC(days);

      const snap = await db
        .collection("cestas")
        .where(DATE_FIELD, ">=", startTs)
        .where(DATE_FIELD, "<", endTs)
        .get();

      if (snap.empty) continue;

      const isAgendada = (d: FirebaseFirestore.QueryDocumentSnapshot) =>
        normEstado(d.get(ESTADO_FIELD)) === "agendada";

      // ✅ 1) Se NÃO estiver Agendada -> não envia e ainda limpa flags para false
      const notAgendadaDocs = snap.docs.filter(d => !isAgendada(d));
      if (notAgendadaDocs.length > 0) {
        const b = db.batch();
        notAgendadaDocs.forEach(d => {
          b.update(d.ref, {
            notifRecolha7d: false,
            notifRecolha1d: false,
            notifRecolha0d: false,
          });
        });
        await b.commit();
      }

      // ✅ 2) Só as Agendadas e ainda não notificadas neste “offset”
      const docs = snap.docs.filter(d => isAgendada(d) && d.get(flag) !== true);
      if (docs.length === 0) continue;

      // ---------- Funcionários + Admin (tópico) ----------
      const preview = docs
        .slice(0, 5)
        .map(d => String(d.get(APOIADO_FIELD) ?? "apoiado"))
        .join(", ");

      const bodyFunc = docs.length <= 5 ? preview : `${preview} e mais ${docs.length - 5}…`;

      await messaging.send({
        topic: "funcionarios",
        notification: {
          title: `📦 Entregas agendadas — ${label}`,
          body: bodyFunc,
        },
        data: {
          type: "CESTA_ENTREGA_REMINDER",
          whenDays: String(days),
        },
      });

      // ---------- Apoiados (por tokens) ----------
      const uniqueApoiados = Array.from(
        new Set(docs.map(d => String(d.get(APOIADO_FIELD) ?? "").trim()).filter(Boolean))
      );

      for (const apoiadoId of uniqueApoiados) {
        const tokensSnap = await db
          .collection("apoiados")
          .doc(apoiadoId)
          .collection("fcmTokens")
          .get();

        const tokens = tokensSnap.docs.map(t => t.id).filter(Boolean);
        if (tokens.length === 0) continue;

        const anyDoc = docs.find(d => String(d.get(APOIADO_FIELD) ?? "").trim() === apoiadoId);
        const dt = (anyDoc?.get(DATE_FIELD) as Timestamp | undefined)?.toDate();

        const datePt = dt ? dt.toLocaleDateString("pt-PT", { timeZone: "Europe/Lisbon" }) : "";
        const timePt = dt
          ? dt.toLocaleTimeString("pt-PT", { timeZone: "Europe/Lisbon", hour: "2-digit", minute: "2-digit" })
          : "09:00";

        const body =
          days === 0 ? `Hoje (${datePt}) tens entrega agendada. Hora: ${timePt}.` :
          days === 1 ? `Amanhã (${datePt}) tens entrega agendada. Hora: ${timePt}.` :
          `Daqui a 7 dias (${datePt}) tens entrega agendada. Hora: ${timePt}.`;

        await messaging.sendEachForMulticast({
          tokens,
          notification: { title: "📦 Lembrete de entrega agendada", body },
          data: {
            type: "CESTA_ENTREGA_REMINDER",
            whenDays: String(days),
            apoiadoId,
          },
        });
      }

      // ---------- Marcar como notificado ----------
      const batch = db.batch();
      docs.forEach(d => {
        batch.update(d.ref, {
          [flag]: true,
          [flagEm]: Timestamp.now(),
        });
      });
      await batch.commit();
    }
  }
);

// ---------------------------
// CHAT: Notificações + resumo no documento do apoiado
// ---------------------------

export const onChatMessageCreated = onDocumentCreated(
  {
    document: "apoiados/{apoiadoId}/chat/{messageId}",
    region: "europe-southwest1",
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const apoiadoId = event.params.apoiadoId;
    const data: any = snapshot.data() || {};

    const text = String(data.text || "").trim();
    const senderRoleRaw = String(data.senderRole || "").trim();
    const senderRole = senderRoleRaw.toUpperCase();
    const senderName = String(data.senderName || "").trim();

    // createdAt pode ser Timestamp (serverTimestamp resolvido) ou pode ainda não existir.
    const createdAt = (data.createdAt || data.createdAtClient || Timestamp.now()) as any;

    const db = getFirestore();
    const apoiadoRef = db.collection("apoiados").doc(apoiadoId);

    // Atualiza resumo no documento do apoiado para permitir ordenação e contadores de não lidas.
    const resumoUpdate: any = {
      chatLastMessageAt: createdAt,
      chatLastMessageText: text.substring(0, 1000),
      chatLastSenderName: senderName,
      chatLastSenderRole: senderRoleRaw,
    };

    if (senderRole === "APOIADO") {
      resumoUpdate.chatUnreadForStaff = FieldValue.increment(1);
    } else {
      resumoUpdate.chatUnreadForApoiado = FieldValue.increment(1);
    }

    await apoiadoRef.set(resumoUpdate, { merge: true });

    // ---------------------------
    // Notificações
    // ---------------------------
    const body = text.length > 140 ? `${text.substring(0, 140)}…` : text;

    if (senderRole === "APOIADO") {
      // Envia para todos os funcionários (tópico)
      let apoiadoNome = senderName;
      try {
        const doc = await apoiadoRef.get();
        apoiadoNome = (doc.data()?.nome as string) || senderName || apoiadoId;
      } catch (_) {
        // ignore
      }

      await getMessaging().send({
        topic: "funcionarios",
        notification: {
          title: `Nova mensagem de ${apoiadoNome}`,
          body,
        },
        data: {
          type: "CHAT_MESSAGE",
          apoiadoId,
        },
      });
      return;
    }

    // Caso contrário (FUNCIONARIO/ADMIN), envia para o apoiado (tokens guardados em fcmTokens)
    const tokensSnap = await apoiadoRef.collection("fcmTokens").get();
    const tokens = tokensSnap.docs.map((t) => t.id).filter(Boolean);
    if (tokens.length === 0) return;

    const res = await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: senderName ? `Nova mensagem de ${senderName}` : "Nova mensagem",
        body,
      },
      data: {
        type: "CHAT_MESSAGE",
        apoiadoId,
      },
    });

    // Limpar tokens inválidos
    const batch = db.batch();
    res.responses.forEach((r, idx) => {
      if (!r.success) {
        const code = (r.error as any)?.code ?? "";
        if (code.includes("registration-token-not-registered") || code.includes("invalid-argument")) {
          batch.delete(tokensSnap.docs[idx].ref);
        }
      }
    });

    await batch.commit();
  }
);


// quando estadoCesta passa a “Entregue” → flags a false

export const resetNotifFlagsOnEntregue = onDocumentUpdated(
  { document: "cestas/{cestaId}", region: "europe-southwest1" },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    const norm = (v: any) => String(v ?? "").trim().toLowerCase();

    const beforeEstado = norm(before.estadoCesta);
    const afterEstado = norm(after.estadoCesta);

    // só quando muda para Entregue
    if (beforeEstado === "entregue" || afterEstado !== "entregue") return;

    await event.data!.after.ref.update({
      notifRecolha7d: false,
      notifRecolha1d: false,
      notifRecolha0d: false,
      // se quiseres limpar timestamps também:
      // notifRecolha7dEm: null,
      // notifRecolha1dEm: null,
      // notifRecolha0dEm: null,
    });
  }
);



//se dataAgendada mudar → reset flags (reagendamento)

export const resetNotifFlagsOnDataAgendadaChange = onDocumentUpdated(
  { document: "cestas/{cestaId}", region: "europe-southwest1" },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    const b = before.dataAgendada as Timestamp | undefined;
    const a = after.dataAgendada as Timestamp | undefined;
    if (!b || !a) return;

    if (b.toMillis() === a.toMillis()) return; // não mudou

    // mudou → reset
    await event.data!.after.ref.update({
      notifRecolha7d: false,
      notifRecolha1d: false,
      notifRecolha0d: false,
    });
  }
);



export const notifyApoiadoCestaAgendadaOnCreate = onDocumentCreated(
  { document: "cestas/{cestaId}", region: "europe-southwest1" },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const cestaId = String(event.params.cestaId);

    // 🔄 Ping silencioso (data-only) para atualizar widgets de Funcionários/Admin
    // (Não cria notificação visível, mas dispara onMessageReceived no Android mesmo em background.)
    try {
      await getMessaging().send({
        topic: "funcionarios",
        android: { priority: "high" },
        data: {
          type: "WIDGET_REFRESH",
          trigger: "CESTA_CREATED",
          cestaId,
        },
      });
    } catch (e) {
      console.error("WIDGET_REFRESH (funcionarios) falhou", e);
    }

    const apoiadoId = String(data.apoiadoID ?? "").trim(); // ✅ no teu Firestore
    if (!apoiadoId) return;

    // Se por algum motivo já vier como Entregue, não envia
    const estado = String(data.estadoCesta ?? "").trim().toLowerCase();
    if (estado === "entregue") return;

    const ts = data.dataAgendada as Timestamp | undefined; // ✅ base para data de entrega
    const dt = ts?.toDate();

    const datePt = dt
      ? dt.toLocaleDateString("pt-PT", { timeZone: "Europe/Lisbon" })
      : "breve";
    const timePt = dt
      ? dt.toLocaleTimeString("pt-PT", { timeZone: "Europe/Lisbon", hour: "2-digit", minute: "2-digit" })
      : "";

    const msg =
      timePt
        ? `Foi agendada uma entrega para ${datePt} às ${timePt}.`
        : `Foi agendada uma entrega para ${datePt}.`;

    const db = getFirestore();

    // Buscar tokens do apoiado
    const tokensSnap = await db
      .collection("apoiados")
      .doc(apoiadoId)
      .collection("fcmTokens")
      .get();

    const tokens = tokensSnap.docs.map((d) => d.id).filter(Boolean);
    if (tokens.length === 0) return;

    // 1) Notificação visível (mantém comportamento atual)
    await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "📦 Entrega agendada",
        body: msg,
      },
      data: {
        type: "CESTA_AGENDADA",
        cestaId,
        apoiadoId,
      },
    });

    // 2) Data-only ping extra p/ atualizar widget mesmo em background
    try {
      await getMessaging().sendEachForMulticast({
        tokens,
        android: { priority: "high" },
        data: {
          type: "WIDGET_REFRESH",
          trigger: "CESTA_CREATED",
          cestaId,
          apoiadoId,
        },
      });
    } catch (e) {
      console.error("WIDGET_REFRESH (apoiado tokens) falhou", e);
    }
  }
);

export const notifyApoiadoContaExpirada = onSchedule(
  { schedule: "0 9 * * *", timeZone: "Europe/Lisbon" }, //como esta é tds os dias as 9 ("0 9 * * *") |para testar a cada minuto usar: "* * * * *"|
  async () => {
    const db = getFirestore();
    const messaging = getMessaging();

    const now = Timestamp.now();

    // procura apoiados cuja validade já passou
    const snap = await db
      .collection("apoiados")
      .where("validadeConta", "<", now)
      .get();

    if (snap.empty) return;

    const toNotify = snap.docs.filter(d => d.get("notifContaExpirada") !== true);

    for (const doc of toNotify) {
      const apoiadoId = doc.id;

      // tokens do apoiado
      const tokensSnap = await db
        .collection("apoiados")
        .doc(apoiadoId)
        .collection("fcmTokens")
        .get();

      const tokens = tokensSnap.docs.map(t => t.id).filter(Boolean);
      if (tokens.length === 0) {
        // marca na mesma para não tentar para sempre (opcional)
        await doc.ref.update({ notifContaExpirada: true, notifContaExpiradaEm: Timestamp.now() });
        continue;
      }

      const res = await messaging.sendEachForMulticast({
        tokens,
        notification: {
          title: "⚠️ A sua conta expirou",
          body: "Entre na app renovar a sua conta.",
        },
        data: {
          type: "ACCOUNT_EXPIRED",
          apoiadoId,
        },
      });

      // limpar tokens inválidos (boa prática)
      const batch = db.batch();
      res.responses.forEach((r, idx) => {
        if (!r.success) {
          const code = (r.error as any)?.code ?? "";
          if (code.includes("registration-token-not-registered") || code.includes("invalid-argument")) {
            batch.delete(tokensSnap.docs[idx].ref);
          }
        }
      });

      // marcar como “já notificado”
      batch.update(doc.ref, {
        notifContaExpirada: true,
        notifContaExpiradaEm: Timestamp.now(),
        // opcional: estadoConta: "Expirada"
        // estadoConta: "Expirada",
      });

      await batch.commit();
    }
  }
);
