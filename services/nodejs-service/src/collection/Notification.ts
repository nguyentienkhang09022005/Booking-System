import mongoose, { Schema, Document } from "mongoose";

export interface INotification extends Document {
  idNotification: string;
  idUser: string;
  title: string;
  content?: string;
  type: "booking" | "message" | "evaluate" | "favorite" | "system";
  refId?: string | null;
  isRead: boolean;
  createdAt: Date;
}

const NotificationSchema = new Schema<INotification>({
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
  createdAt: { type: Date, required: true, default: Date.now },
});

export const NotificationModel = mongoose.model<INotification>("Notification", NotificationSchema, "notifications");
