import { MessageModel } from "../../collection/Message.ts";
import type { Message } from "../../type/Chat.ts";

export const getMessageList = async (
  idSender: string,
  idReceiver: string
): Promise<Message[]> => {
  const messages = await MessageModel.find({
    $or: [
      { idSender, idReceiver },
      { idSender: idReceiver, idReceiver: idSender }
    ]
  })
    .sort({ createdAt: 1 })
    .lean();

  return messages as Message[];
};

export default getMessageList;
