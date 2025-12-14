export interface Receiver {
  firstname: string;
  lastname: string;
  email: string;
}

export interface SendMessageInput {
  idSender: string;
  idReceiver: string;
  message: string;
  type?: "text" | "image" | "file";
}

export interface Message {
  idMessage: string;
  idSender: string;
  idReceiver?: string;
  message: string;
  type?: "text" | "image" | "file";
  status?: "sent" | "seen";
  createdAt: Date;
}
