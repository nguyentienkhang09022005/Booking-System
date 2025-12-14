import express from "express";
import { getNotifications } from "../services/notification/getNotifications.ts";
import { readNotification } from "../services/notification/readNotification.ts";
import { deleteNotification } from "../services/notification/deleteNotification.ts";

class NotificationController {
    async getNotifications(req: express.Request, res: express.Response) {
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

    async markAsRead(req: express.Request, res: express.Response) {
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

    async deleteNotification(req: express.Request, res: express.Response) {
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
}

export const notificationController = new NotificationController();