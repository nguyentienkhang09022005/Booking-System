import sql from "../../config/postgre.ts";
import { MessageModel } from "../../collection/Message.ts";
export const getReceivers = async (idSender) => {
    console.log(idSender);
    const messages = await MessageModel.find({
        $or: [
            { idSender },
            { idReceiver: idSender }
        ]
    }).lean();
    const userIds = new Set();
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
    const receivers = await sql `
    SELECT id_user, first_name, last_name, email
    FROM users
    WHERE id_user = ANY(${allUserIds})
  `;
    return receivers;
};
export default getReceivers;
//# sourceMappingURL=getReceivers.js.map