import mongoose, { Document } from "mongoose";
export interface IMessage extends Document {
    idMessage: string;
    idSender: string;
    idReceiver?: string;
    message: string;
    type?: "text" | "image" | "file";
    status?: "sent" | "seen";
    createdAt: Date;
}
export declare const MessageModel: mongoose.Model<IMessage, {}, {}, {}, mongoose.Document<unknown, {}, IMessage, {}, mongoose.DefaultSchemaOptions> & IMessage & Required<{
    _id: mongoose.Types.ObjectId;
}> & {
    __v: number;
}, any, IMessage>;
//# sourceMappingURL=Message.d.ts.map