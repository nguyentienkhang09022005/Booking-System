import express from "express";
import cors from "cors";
import cookieParser from "cookie-parser";
import { createServer } from "http";
import swaggerUi from "swagger-ui-express"; 

import route from "./routes/index.ts";  
import { connectMongo } from "./config/mongo.ts";
import { initSocket } from "./config/socket.ts";
import swaggerSpec from "./config/swagger.ts";

const PORT = process.env.PORT || 3000; 

const app = express();
const httpServer = createServer(app);

app.use(express.urlencoded({extended: true}));
app.use(express.json());
app.use(cookieParser());
// app.use(cors());

app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(swaggerSpec));

route(app);

initSocket(httpServer);

httpServer.listen(Number(PORT), '0.0.0.0', async () => {
  await connectMongo();
  console.log(`Node.js service is running on port ${PORT}`);
  console.log(`Swagger docs available at /api-docs`);
  console.log(`Socket.IO is ready for connections`);
});