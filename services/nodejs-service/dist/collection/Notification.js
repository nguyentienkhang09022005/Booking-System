import mongoose, { Schema, Document } from "mongoose";
const NotificationSchema = new Schema({
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
export const NotificationModel = mongoose.model("Notification", NotificationSchema, "notifications");
//# sourceMappingURL=Notification.js.map