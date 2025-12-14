import { chatController } from "../controllers/ChatController.ts";
import express from "express";
const router = express.Router();
//[/chat/]
/**
 * @swagger
 * /chat/receivers:
 *   get:
 *     summary: Get list of receivers for a sender
 *     tags: [Chat]
 *     parameters:
 *       - in: query
 *         name: idSender
 *         required: true
 *         schema:
 *           type: string
 *         description: ID of the sender
 *     responses:
 *       200:
 *         description: List of receivers
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/Receiver'
 *       400:
 *         description: Bad request
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Error'
 *       500:
 *         description: Internal server error
 */
router.get("/receivers", chatController.getReceivers);
/**
 * @swagger
 * /chat:
 *   get:
 *     summary: Get chat messages between two users
 *     tags: [Chat]
 *     parameters:
 *       - in: query
 *         name: idSender
 *         required: true
 *         schema:
 *           type: string
 *         description: ID of the sender
 *       - in: query
 *         name: idReceiver
 *         required: true
 *         schema:
 *           type: string
 *         description: ID of the receiver
 *     responses:
 *       200:
 *         description: List of messages
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/Message'
 *       400:
 *         description: Bad request
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Error'
 *       500:
 *         description: Internal server error
 */
router.get("/", chatController.getChats);
/**
 * @swagger
 * /chat:
 *   post:
 *     summary: Send a new message
 *     tags: [Chat]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/SendMessageRequest'
 *     responses:
 *       201:
 *         description: Message sent successfully
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Message'
 *       400:
 *         description: Bad request
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Error'
 *       500:
 *         description: Internal server error
 */
router.post("/", chatController.sendMessage);
export default router;
//# sourceMappingURL=chat.js.map