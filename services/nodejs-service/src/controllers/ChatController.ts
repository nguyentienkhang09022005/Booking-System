import express from "express";
import { getReceivers } from "../services/chat/getReceivers.ts";
import { getMessageList } from "../services/chat/getMessageList.ts";
import { sendMessage } from "../services/chat/sendMessage.ts";

class ChatController {

    async getReceivers(req: express.Request, res: express.Response) {
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

    async getChats(req: express.Request, res: express.Response) {
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

    async sendMessage(req: express.Request, res: express.Response) {
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
}

export const chatController = new ChatController();