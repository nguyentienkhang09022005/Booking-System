import { NotificationModel } from "../../collection/Notification.ts";
import type { INotification } from "../../collection/Notification.ts";

export const getNotifications = async (idUser: string): Promise<INotification[]> => {
  const notifications = await NotificationModel.find({ idUser })
    .sort({ createdAt: -1 })
    .lean();

  return notifications as INotification[];
};

export default getNotifications;
