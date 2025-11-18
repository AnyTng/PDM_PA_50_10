
import * as functions from "firebase-functions";
import express from "express";
import cors from "cors";
import routes from "./routes";



const app = express();

app.use(cors({ origin: true }));
app.use(express.json());
app.use("/api", routes);

export const api = functions.https.onRequest(app);