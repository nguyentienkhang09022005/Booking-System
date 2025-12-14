import mongoose, { Document } from "mongoose";
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
export declare const NotificationModel: mongoose.Model<INotification, {}, {}, {}, mongoose.Document<unknown, {}, INotification, {}, mongoose.DefaultSchemaOptions> & INotification & Required<{
    _id: mongoose.Types.ObjectId;
}> & {
    __v: number;
}, any, INotification>;
//# sourceMappingURL=Notification.d.ts.map