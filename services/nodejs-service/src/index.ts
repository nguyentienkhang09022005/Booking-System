import express from "express";
import cors from "cors";
import cookieParser from "cookie-parser";
import { createServer } from "http";

import route from "./routes/index.ts";
import { connectMongo } from "./config/mongo.ts";
import { initSocket } from "./config/socket.ts";

const PORT = 3000;
const app = express();
const httpServer = createServer(app);

app.use(express.urlencoded({extended: true}));
app.use(express.json());
app.use(cookieParser());
app.use(cors());

route(app);

// Khởi tạo Socket.IO
initSocket(httpServer);

httpServer.listen(PORT, async () => {
  await connectMongo();
  console.log(`Node.js service is running on port ${PORT}`);
  console.log(`Socket.IO is ready for connections`);
});
