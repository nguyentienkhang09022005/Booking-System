import { NotificationModel } from "../../collection/Notification.ts";
export const getNotifications = async (idUser) => {
    const notifications = await NotificationModel.find({ idUser })
        .sort({ createdAt: -1 })
        .lean();
    return notifications;
};
export default getNotifications;
//# sourceMappingURL=getNotifications.js.map