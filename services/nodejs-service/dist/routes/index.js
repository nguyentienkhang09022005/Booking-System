import express from "express";
import swaggerUi from "swagger-ui-express";
import chatRouter from "./chat.ts";
import notificationRouter from "./notification.ts";
import swaggerSpec from "../config/swagger.ts";
export default function route(app) {
    // Swagger documentation
    app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(swaggerSpec));
    // [/chat]
    app.use("/chat", chatRouter);
    // [/notification]
    app.use("/notification", notificationRouter);
}
//# sourceMappingURL=index.js.map