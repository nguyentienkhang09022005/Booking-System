import { Server as SocketIOServer, Socket } from "socket.io";
import { Server as HTTPServer } from "http";
let io;
const userSockets = new Map();
export const initSocket = (httpServer) => {
    io = new SocketIOServer(httpServer, {
        cors: {
            origin: "*",
            methods: ["GET", "POST"],
        },
    });
    io.on("connection", (socket) => {
        console.log(`Socket connected: ${socket.id}`);
        socket.on("register", (userId) => {
            if (!userId)
                return;
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
                createdAt: new Date(),
            });
            console.log(`Message sent from ${idSender} to ${idReceiver}`);
        });
        socket.on("typing", (data) => {
            io.to(data.idReceiver).emit("userTyping", {
                idSender: data.idSender,
            });
        });
        socket.on("stopTyping", (data) => {
            io.to(data.idReceiver).emit("userStopTyping", {
                idSender: data.idSender,
            });
        });
        socket.on("markMessageSeen", (data) => {
            io.to(data.idSender).emit("messageSeen", {
                idMessage: data.idMessage,
                seenBy: data.idReceiver,
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
                    }
                    else {
                        userSockets.set(userId, sockets);
                    }
                }
            });
        });
    });
    return io;
};
export const emitNotification = (userId, notification) => {
    if (io) {
        io.to(userId).emit("newNotification", notification);
        console.log(`Notification sent to user ${userId}`);
    }
};
export const emitMessage = (idReceiver, message) => {
    if (io) {
        io.to(idReceiver).emit("newMessage", message);
    }
};
export const getIO = () => io;
//# sourceMappingURL=socket.js.map