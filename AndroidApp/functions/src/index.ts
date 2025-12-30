import { onSchedule } from "firebase-functions/v2/scheduler";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

initializeApp();

export const notifyExpiringProducts7d = onSchedule(
  { schedule: "* * * * *", timeZone: "Europe/Lisbon" }, // ✅ teste: a cada 1 min
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
