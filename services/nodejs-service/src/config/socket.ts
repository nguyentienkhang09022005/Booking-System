import { Server as SocketIOServer, Socket, Namespace } from "socket.io";
import { Server as HTTPServer } from "http";

let io: SocketIOServer;
let hubs: Namespace;

const userSockets = new Map<string, string[]>();

export const initSocket = (httpServer: HTTPServer) => {
  io = new SocketIOServer(httpServer, {
    cors: {
      origin: "*",
      methods: ["GET", "POST"],
    },
  });

  // ✅ Namespace /hubs
  hubs = io.of("/hubs");

  hubs.on("connection", (socket: Socket) => {
    console.log(`Socket connected (/hubs): ${socket.id}`);

    socket.on("register", (userId: string) => {
      if (!userId) return;

      const existingSockets = userSockets.get(userId) || [];
      if (!existingSockets.includes(socket.id)) {
        existingSockets.push(socket.id);
        userSockets.set(userId, existingSockets);
      }

      socket.join(userId); // room name = userId (trong namespace /hubs)
      console.log(`User ${userId} registered with socket ${socket.id}`);
    });

    socket.on(
      "sendMessage",
      (data: {
        idSender: string;
        idReceiver: string;
        message: string;
        type?: "text" | "image" | "file";
      }) => {
        const { idSender, idReceiver, message, type = "text" } = data;

        // ✅ emit trong /hubs
        hubs.to(idReceiver).emit("newMessage", {
          idSender,
          idReceiver,
          message,
          type,
          createdAt: new Date(),
        });

        console.log(`Message sent from ${idSender} to ${idReceiver}`);
      }
    );

    socket.on("typing", (data: { idSender: string; idReceiver: string }) => {
      hubs.to(data.idReceiver).emit("userTyping", { idSender: data.idSender });
    });

    socket.on("stopTyping", (data: { idSender: string; idReceiver: string }) => {
      hubs.to(data.idReceiver).emit("userStopTyping", { idSender: data.idSender });
    });

    socket.on(
      "markMessageSeen",
      (data: { idSender: string; idReceiver: string; idMessage: string }) => {
        hubs.to(data.idSender).emit("messageSeen", {
          idMessage: data.idMessage,
          seenBy: data.idReceiver,
        });
      }
    );  

    socket.on("disconnect", () => {
      console.log(`Socket disconnected: ${socket.id}`);

      userSockets.forEach((sockets, userId) => {
        const index = sockets.indexOf(socket.id);
        if (index !== -1) {
          sockets.splice(index, 1);
          if (sockets.length === 0) userSockets.delete(userId);
          else userSockets.set(userId, sockets);
        }
      });
    });
  });

  return io;
};

// ✅ các emit helper nên dùng hubs (namespace /hubs) thay vì io
export const emitNotification = (
  userId: string,
  notification: {
    idNotification: string;
    title: string;
    content?: string;
    type: "booking" | "message" | "evaluate" | "favorite" | "system";
    refId?: string | null;
    createdAt: Date;
  }
) => {
  if (hubs) {
    hubs.to(userId).emit("newNotification", notification);
    console.log(`Notification sent to user ${userId}`);
  }
};

export const emitMessage = (
  idReceiver: string,
  message: {
    idMessage: string;
    idSender: string;
    idReceiver: string;
    message: string;
    type?: "text" | "image" | "file";
    status?: "sent" | "seen";
    createdAt: Date;
  }
) => {
  if (hubs) {
    hubs.to(idReceiver).emit("newMessage", message);
  }
};

export const getIO = () => io;
export const getHubs = () => hubs;
