import express from "express";
declare class ChatController {
    getReceivers(req: express.Request, res: express.Response): Promise<express.Response<any, Record<string, any>>>;
    getChats(req: express.Request, res: express.Response): Promise<express.Response<any, Record<string, any>>>;
    sendMessage(req: express.Request, res: express.Response): Promise<express.Response<any, Record<string, any>>>;
}
export declare const chatController: ChatController;
export {};
//# sourceMappingURL=ChatController.d.ts.map