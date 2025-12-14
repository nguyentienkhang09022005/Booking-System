import mongoose, { Schema, Document } from "mongoose";
const MessageSchema = new Schema({
    idMessage: { type: String, required: true },
    idSender: { type: String, required: true },
    idReceiver: { type: String },
    message: { type: String, required: true },
    type: { type: String, enum: ["text", "image", "file"], default: "text" },
    status: { type: String, enum: ["sent", "seen"], default: "sent" },
    createdAt: { type: Date, required: true, default: Date.now },
});
export const MessageModel = mongoose.model("Message", MessageSchema, "messages");
//# sourceMappingURL=Message.js.map