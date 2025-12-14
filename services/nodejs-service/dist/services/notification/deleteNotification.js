import { NotificationModel } from "../../collection/Notification.ts";
export const deleteNotification = async (idNotification) => {
    const result = await NotificationModel.deleteOne({ idNotification });
    return result.deletedCount > 0;
};
export default deleteNotification;
//# sourceMappingURL=deleteNotification.js.map