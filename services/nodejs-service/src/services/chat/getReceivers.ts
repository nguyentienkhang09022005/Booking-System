import sql from "../../config/postgre.ts";
import { MessageModel } from "../../collection/Message.ts";
import type { Receiver } from "../../type/Chat.ts";

export const getReceivers = async (idSender: string): Promise<Receiver[]> => {
  const messages = await MessageModel.find({
    $or: [
      { idSender },
      { idReceiver: idSender }
    ]
  }).lean();

  const userIds = new Set<string>();
  messages.forEach(msg => {
    if (msg.idSender && msg.idSender !== idSender && msg.idSender !== "undefined") {
      userIds.add(msg.idSender);
    }
    if (msg.idReceiver && msg.idReceiver !== idSender && msg.idReceiver !== "undefined") {
      userIds.add(msg.idReceiver);
    }
  });

  const allUserIds = Array.from(userIds).filter(id => id && id !== "undefined" && id.trim() !== "");

  if (allUserIds.length === 0) {
    return [];
  }

  const receivers = await sql`
    SELECT id_user AS "idUser", first_name AS "firstName", last_name AS "lastName", email
    FROM users
    WHERE id_user = ANY(${allUserIds})
  `;

  return receivers as Receiver[];
};

export default getReceivers;
