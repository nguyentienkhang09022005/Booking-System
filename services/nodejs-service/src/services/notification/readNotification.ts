import { NotificationModel } from "../../collection/Notification.ts";

export const readNotification = async (idNotification: string): Promise<boolean> => {
  const result = await NotificationModel.updateOne(
    { idNotification },
    { $set: { isRead: true } }
  );

  return result.modifiedCount > 0;
};

export default readNotification;
