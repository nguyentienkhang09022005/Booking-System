import { NotificationModel } from "../../collection/Notification.ts";

export const deleteNotification = async (idNotification: string): Promise<boolean> => {
  const result = await NotificationModel.deleteOne({ idNotification });

  return result.deletedCount > 0;
};

export default deleteNotification;
