import express from "express";
declare class NotificationController {
    getNotifications(req: express.Request, res: express.Response): Promise<express.Response<any, Record<string, any>>>;
    markAsRead(req: express.Request, res: express.Response): Promise<express.Response<any, Record<string, any>>>;
    deleteNotification(req: express.Request, res: express.Response): Promise<express.Response<any, Record<string, any>>>;
}
export declare const notificationController: NotificationController;
export {};
//# sourceMappingURL=NotificationController.d.ts.map