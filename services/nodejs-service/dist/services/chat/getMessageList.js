import { MessageModel } from "../../collection/Message.ts";
export const getMessageList = async (idSender, idReceiver) => {
    const messages = await MessageModel.find({
        $or: [
            { idSender, idReceiver },
            { idSender: idReceiver, idReceiver: idSender }
        ]
    })
        .sort({ createdAt: 1 })
        .lean();
    return messages;
};
export default getMessageList;
//# sourceMappingURL=getMessageList.js.map