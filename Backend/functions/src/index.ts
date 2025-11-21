import * as functions from "firebase-functions";
import express from "express";
import cors from "cors";
import { router } from "./routes";

const app = express();
app.set("trust proxy", 1);

app.use(cors({ origin: true }));
app.use("/api", router);

export const api = functions.https.onRequest(app);