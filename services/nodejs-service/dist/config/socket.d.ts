import { Server as SocketIOServer } from "socket.io";
import { Server as HTTPServer } from "http";
export declare const initSocket: (httpServer: HTTPServer) => SocketIOServer<import("socket.io").DefaultEventsMap, import("socket.io").DefaultEventsMap, import("socket.io").DefaultEventsMap, any>;
export declare const emitNotification: (userId: string, notification: {
    idNotification: string;
    title: string;
    content?: string;
    type: "booking" | "message" | "evaluate" | "favorite" | "system";
    refId?: string | null;
    createdAt: Date;
}) => void;
export declare const emitMessage: (idReceiver: string, message: {
    idMessage: string;
    idSender: string;
    idReceiver: string;
    message: string;
    type?: "text" | "image" | "file";
    status?: "sent" | "seen";
    createdAt: Date;
}) => void;
export declare const getIO: () => SocketIOServer<import("socket.io").DefaultEventsMap, import("socket.io").DefaultEventsMap, import("socket.io").DefaultEventsMap, any>;
//# sourceMappingURL=socket.d.ts.map