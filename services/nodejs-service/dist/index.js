// src/index.ts
import express3 from "express";
import cors from "cors";
import cookieParser from "cookie-parser";
import { createServer } from "http";

// src/routes/index.ts
import swaggerUi from "swagger-ui-express";

// src/config/postgre.ts
import { neon } from "@neondatabase/serverless";
import dotenv from "dotenv";
dotenv.config();
var sql = neon(process.env.POSTGRESQL_CONNECTION);
var postgre_default = sql;

// src/collection/Message.ts
import mongoose, { Schema } from "mongoose";
var MessageSchema = new Schema({
  idMessage: { type: String, required: true },
  idSender: { type: String, required: true },
  idReceiver: { type: String },
  message: { type: String, required: true },
  type: { type: String, enum: ["text", "image", "file"], default: "text" },
  status: { type: String, enum: ["sent", "seen"], default: "sent" },
  createdAt: { type: Date, required: true, default: Date.now }
});
var MessageModel = mongoose.model("Message", MessageSchema, "messages");

// src/services/chat/getReceivers.ts
var getReceivers = async (idSender) => {
  console.log(idSender);
  const messages = await MessageModel.find({
    $or: [
      { idSender },
      { idReceiver: idSender }
    ]
  }).lean();
  const userIds = /* @__PURE__ */ new Set();
  messages.forEach((msg) => {
    if (msg.idSender && msg.idSender !== idSender && msg.idSender !== "undefined") {
      userIds.add(msg.idSender);
    }
    if (msg.idReceiver && msg.idReceiver !== idSender && msg.idReceiver !== "undefined") {
      userIds.add(msg.idReceiver);
    }
  });
  const allUserIds = Array.from(userIds).filter((id) => id && id !== "undefined" && id.trim() !== "");
  if (allUserIds.length === 0) {
    return [];
  }
  const receivers = await postgre_default`
    SELECT id_user, first_name, last_name, email
    FROM users
    WHERE id_user = ANY(${allUserIds})
  `;
  return receivers;
};

// src/services/chat/getMessageList.ts
var getMessageList = async (idSender, idReceiver) => {
  const messages = await MessageModel.find({
    $or: [
      { idSender, idReceiver },
      { idSender: idReceiver, idReceiver: idSender }
    ]
  }).sort({ createdAt: 1 }).lean();
  return messages;
};

// src/collection/Notification.ts
import mongoose2, { Schema as Schema2 } from "mongoose";
var NotificationSchema = new Schema2({
  idNotification: { type: String, required: true },
  idUser: { type: String, required: true },
  title: { type: String, required: true },
  content: { type: String },
  type: {
    type: String,
    enum: ["booking", "message", "evaluate", "favorite", "system"],
    required: true
  },
  refId: { type: String, default: null },
  isRead: { type: Boolean, required: true, default: false },
  createdAt: { type: Date, required: true, default: Date.now }
});
var NotificationModel = mongoose2.model("Notification", NotificationSchema, "notifications");

// src/services/chat/sendMessage.ts
import { v4 as uuidv4 } from "uuid";

// src/config/socket.ts
import { Server as SocketIOServer } from "socket.io";
var io;
var userSockets = /* @__PURE__ */ new Map();
var initSocket = (httpServer2) => {
  io = new SocketIOServer(httpServer2, {
    cors: {
      origin: "*",
      methods: ["GET", "POST"]
    }
  });
  io.on("connection", (socket) => {
    console.log(`Socket connected: ${socket.id}`);
    socket.on("register", (userId) => {
      if (!userId) return;
      const existingSockets = userSockets.get(userId) || [];
      if (!existingSockets.includes(socket.id)) {
        existingSockets.push(socket.id);
        userSockets.set(userId, existingSockets);
      }
      socket.join(userId);
      console.log(`User ${userId} registered with socket ${socket.id}`);
    });
    socket.on("sendMessage", (data) => {
      const { idSender, idReceiver, message, type = "text" } = data;
      io.to(idReceiver).emit("newMessage", {
        idSender,
        idReceiver,
        message,
        type,
        createdAt: /* @__PURE__ */ new Date()
      });
      console.log(`Message sent from ${idSender} to ${idReceiver}`);
    });
    socket.on("typing", (data) => {
      io.to(data.idReceiver).emit("userTyping", {
        idSender: data.idSender
      });
    });
    socket.on("stopTyping", (data) => {
      io.to(data.idReceiver).emit("userStopTyping", {
        idSender: data.idSender
      });
    });
    socket.on("markMessageSeen", (data) => {
      io.to(data.idSender).emit("messageSeen", {
        idMessage: data.idMessage,
        seenBy: data.idReceiver
      });
    });
    socket.on("disconnect", () => {
      console.log(`Socket disconnected: ${socket.id}`);
      userSockets.forEach((sockets, userId) => {
        const index = sockets.indexOf(socket.id);
        if (index !== -1) {
          sockets.splice(index, 1);
          if (sockets.length === 0) {
            userSockets.delete(userId);
          } else {
            userSockets.set(userId, sockets);
          }
        }
      });
    });
  });
  return io;
};
var emitNotification = (userId, notification) => {
  if (io) {
    io.to(userId).emit("newNotification", notification);
    console.log(`Notification sent to user ${userId}`);
  }
};
var emitMessage = (idReceiver, message) => {
  if (io) {
    io.to(idReceiver).emit("newMessage", message);
  }
};

// src/services/chat/sendMessage.ts
var sendMessage = async (input) => {
  const { idSender, idReceiver, message, type = "text" } = input;
  const newMessage = new MessageModel({
    idMessage: uuidv4(),
    idSender,
    idReceiver,
    message,
    type,
    status: "sent",
    createdAt: /* @__PURE__ */ new Date()
  });
  await newMessage.save();
  const notification = new NotificationModel({
    idNotification: uuidv4(),
    idUser: idReceiver,
    title: "Tin nh\u1EAFn m\u1EDBi",
    content: message.length > 50 ? message.substring(0, 50) + "..." : message,
    type: "message",
    refId: newMessage.idMessage,
    isRead: false,
    createdAt: /* @__PURE__ */ new Date()
  });
  await notification.save();
  const messageData = {
    idMessage: newMessage.idMessage,
    idSender: newMessage.idSender,
    idReceiver: newMessage.idReceiver ?? idReceiver,
    message: newMessage.message,
    type: newMessage.type ?? "text",
    status: newMessage.status ?? "sent",
    createdAt: newMessage.createdAt
  };
  emitMessage(idReceiver, {
    idMessage: messageData.idMessage,
    idSender: messageData.idSender,
    idReceiver,
    message: messageData.message,
    type: messageData.type ?? "text",
    status: messageData.status ?? "sent",
    createdAt: messageData.createdAt
  });
  emitNotification(idReceiver, {
    idNotification: notification.idNotification,
    title: notification.title,
    content: notification.content ?? "",
    type: notification.type,
    refId: notification.refId ?? null,
    createdAt: notification.createdAt
  });
  return messageData;
};

// src/controllers/ChatController.ts
var ChatController = class {
  async getReceivers(req, res) {
    try {
      const { idSender } = req.query;
      if (!idSender || typeof idSender !== "string") {
        return res.status(400).json({ error: "idSender is required" });
      }
      const receivers = await getReceivers(idSender);
      return res.json(receivers);
    } catch (error) {
      console.error("Error getting receivers:", error);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
  async getChats(req, res) {
    try {
      const { idSender, idReceiver } = req.query;
      if (!idSender || typeof idSender !== "string") {
        return res.status(400).json({ error: "idSender is required" });
      }
      if (!idReceiver || typeof idReceiver !== "string") {
        return res.status(400).json({ error: "idReceiver is required" });
      }
      const messages = await getMessageList(idSender, idReceiver);
      return res.json(messages);
    } catch (error) {
      console.error("Error getting chats:", error);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
  async sendMessage(req, res) {
    try {
      const { idSender, idReceiver, message, type = "text" } = req.body;
      if (!idSender || typeof idSender !== "string") {
        return res.status(400).json({ error: "idSender is required" });
      }
      if (!idReceiver || typeof idReceiver !== "string") {
        return res.status(400).json({ error: "idReceiver is required" });
      }
      if (!message || typeof message !== "string") {
        return res.status(400).json({ error: "message is required" });
      }
      const newMessage = await sendMessage({ idSender, idReceiver, message, type });
      return res.status(201).json(newMessage);
    } catch (error) {
      console.error("Error sending message:", error);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
};
var chatController = new ChatController();

// src/routes/chat.ts
import express from "express";
var router = express.Router();
router.get("/receivers", chatController.getReceivers);
router.get("/", chatController.getChats);
router.post("/", chatController.sendMessage);
var chat_default = router;

// src/services/notification/getNotifications.ts
var getNotifications = async (idUser) => {
  const notifications = await NotificationModel.find({ idUser }).sort({ createdAt: -1 }).lean();
  return notifications;
};

// src/services/notification/readNotification.ts
var readNotification = async (idNotification) => {
  const result = await NotificationModel.updateOne(
    { idNotification },
    { $set: { isRead: true } }
  );
  return result.modifiedCount > 0;
};

// src/services/notification/deleteNotification.ts
var deleteNotification = async (idNotification) => {
  const result = await NotificationModel.deleteOne({ idNotification });
  return result.deletedCount > 0;
};

// src/controllers/NotificationController.ts
var NotificationController = class {
  async getNotifications(req, res) {
    try {
      const { idUser } = req.query;
      if (!idUser || typeof idUser !== "string") {
        return res.status(400).json({ error: "idUser is required" });
      }
      const notifications = await getNotifications(idUser);
      return res.json(notifications);
    } catch (error) {
      console.error("Error getting notifications:", error);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
  async markAsRead(req, res) {
    try {
      const { idNotification } = req.params;
      if (!idNotification || typeof idNotification !== "string") {
        return res.status(400).json({ error: "idNotification is required" });
      }
      const success = await readNotification(idNotification);
      if (!success) {
        return res.status(404).json({ error: "Notification not found" });
      }
      return res.json({ message: "Notification marked as read" });
    } catch (error) {
      console.error("Error marking notification as read:", error);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
  async deleteNotification(req, res) {
    try {
      const { idNotification } = req.params;
      if (!idNotification || typeof idNotification !== "string") {
        return res.status(400).json({ error: "idNotification is required" });
      }
      const success = await deleteNotification(idNotification);
      if (!success) {
        return res.status(404).json({ error: "Notification not found" });
      }
      return res.json({ message: "Notification deleted" });
    } catch (error) {
      console.error("Error deleting notification:", error);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
};
var notificationController = new NotificationController();

// src/routes/notification.ts
import express2 from "express";
var router2 = express2.Router();
router2.get("/", notificationController.getNotifications);
router2.patch("/:idNotification/read", notificationController.markAsRead);
router2.delete("/:idNotification", notificationController.deleteNotification);
var notification_default = router2;

// src/config/swagger.ts
import swaggerJsdoc from "swagger-jsdoc";
var options = {
  definition: {
    openapi: "3.0.0",
    info: {
      title: "Node.js Chat Service API",
      version: "1.0.0",
      description: "API documentation for the Chat Service"
    },
    servers: [
      {
        url: "http://localhost:3000",
        description: "Development server"
      }
    ],
    components: {
      schemas: {
        Receiver: {
          type: "object",
          properties: {
            firstname: {
              type: "string",
              description: "First name of the receiver"
            },
            lastname: {
              type: "string",
              description: "Last name of the receiver"
            },
            email: {
              type: "string",
              description: "Email of the receiver"
            }
          }
        },
        Message: {
          type: "object",
          properties: {
            idMessage: {
              type: "string",
              description: "Message ID"
            },
            idSender: {
              type: "string",
              description: "ID of the sender"
            },
            idReceiver: {
              type: "string",
              description: "ID of the receiver"
            },
            message: {
              type: "string",
              description: "Message content"
            },
            type: {
              type: "string",
              enum: ["text", "image", "file"],
              description: "Message type"
            },
            status: {
              type: "string",
              enum: ["sent", "seen"],
              description: "Message status"
            },
            createdAt: {
              type: "string",
              format: "date-time",
              description: "Creation date"
            }
          }
        },
        SendMessageRequest: {
          type: "object",
          required: ["idSender", "idReceiver", "message"],
          properties: {
            idSender: {
              type: "string",
              description: "ID of the sender"
            },
            idReceiver: {
              type: "string",
              description: "ID of the receiver"
            },
            message: {
              type: "string",
              description: "Message content"
            },
            type: {
              type: "string",
              enum: ["text", "image", "file"],
              description: "Message type (default: text)"
            }
          }
        },
        Error: {
          type: "object",
          properties: {
            error: {
              type: "string",
              description: "Error message"
            }
          }
        },
        Notification: {
          type: "object",
          properties: {
            idNotification: {
              type: "string",
              description: "Notification ID"
            },
            idUser: {
              type: "string",
              description: "User ID"
            },
            title: {
              type: "string",
              description: "Notification title"
            },
            content: {
              type: "string",
              description: "Notification content"
            },
            type: {
              type: "string",
              enum: ["booking", "message", "evaluate", "favorite", "system"],
              description: "Notification type"
            },
            refId: {
              type: "string",
              nullable: true,
              description: "Reference ID"
            },
            isRead: {
              type: "boolean",
              description: "Read status"
            },
            createdAt: {
              type: "string",
              format: "date-time",
              description: "Creation date"
            }
          }
        },
        SuccessMessage: {
          type: "object",
          properties: {
            message: {
              type: "string",
              description: "Success message"
            }
          }
        }
      }
    }
  },
  apis: ["./src/routes/*.ts"]
};
var swaggerSpec = swaggerJsdoc(options);
var swagger_default = swaggerSpec;

// src/routes/index.ts
function route(app2) {
  app2.use("/api-docs", swaggerUi.serve, swaggerUi.setup(swagger_default));
  app2.use("/chat", chat_default);
  app2.use("/notification", notification_default);
}

// src/config/mongo.ts
import mongoose3 from "mongoose";
import dotenv2 from "dotenv";
dotenv2.config();
var connected = false;
async function connectMongo() {
  if (connected) return;
  await mongoose3.connect(process.env.SPRING_DATA_MONGODB_URI, {
    autoIndex: false
  });
  connected = true;
}

// src/index.ts
var PORT = 3e3;
var app = express3();
var httpServer = createServer(app);
app.use(express3.urlencoded({ extended: true }));
app.use(express3.json());
app.use(cookieParser());
app.use(cors());
route(app);
initSocket(httpServer);
httpServer.listen(PORT, async () => {
  await connectMongo();
  console.log(`Node.js service is running on port ${PORT}`);
  console.log(`Socket.IO is ready for connections`);
});
