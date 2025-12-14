import { MessageModel } from "../../collection/Message.ts";
import { NotificationModel } from "../../collection/Notification.ts";
import type { Message, SendMessageInput } from "../../type/Chat.ts";
import { v4 as uuidv4 } from "uuid";
import { emitMessage, emitNotification } from "../../config/socket.ts";

export const sendMessage = async (input: SendMessageInput): Promise<Message> => {
  const { idSender, idReceiver, message, type = "text" } = input;

  const newMessage = new MessageModel({
    idMessage: uuidv4(),
    idSender,
    idReceiver,
    message,
    type,
    status: "sent",
    createdAt: new Date(),
  });

  await newMessage.save();

  const notification = new NotificationModel({
    idNotification: uuidv4(),
    idUser: idReceiver,
    title: "Tin nhắn mới",
    content: message.length > 50 ? message.substring(0, 50) + "..." : message,
    type: "message",
    refId: newMessage.idMessage,
    isRead: false,
    createdAt: new Date(),
  });

  await notification.save();

  const messageData: Message = {
    idMessage: newMessage.idMessage,
    idSender: newMessage.idSender,
    idReceiver: newMessage.idReceiver ?? idReceiver,
    message: newMessage.message,
    type: newMessage.type ?? "text",
    status: newMessage.status ?? "sent",
    createdAt: newMessage.createdAt,
  };

  // Emit real-time message đến receiver
  emitMessage(idReceiver, {
    idMessage: messageData.idMessage,
    idSender: messageData.idSender,
    idReceiver: idReceiver,
    message: messageData.message,
    type: messageData.type ?? "text",
    status: messageData.status ?? "sent",
    createdAt: messageData.createdAt,
  });

  // Emit real-time notification đến receiver
  emitNotification(idReceiver, {
    idNotification: notification.idNotification,
    title: notification.title,
    content: notification.content ?? "",
    type: notification.type,
    refId: notification.refId ?? null,
    createdAt: notification.createdAt,
  });

  return messageData;
};

export default sendMessage;
