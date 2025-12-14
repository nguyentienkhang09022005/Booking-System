import mongoose, { Schema, Document } from "mongoose";

export interface IMessage extends Document {
  idMessage: string;
  idSender: string;
  idReceiver?: string;
  message: string;
  type?: "text" | "image" | "file";
  status?: "sent" | "seen";
  createdAt: Date;
}

const MessageSchema = new Schema<IMessage>({
  idMessage: { type: String, required: true },
  idSender: { type: String, required: true },
  idReceiver: { type: String },
  message: { type: String, required: true },
  type: { type: String, enum: ["text", "image", "file"], default: "text" },
  status: { type: String, enum: ["sent", "seen"], default: "sent" },
  createdAt: { type: Date, required: true, default: Date.now },
});

export const MessageModel = mongoose.model<IMessage>("Message", MessageSchema, "messages");
