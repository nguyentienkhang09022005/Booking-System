export interface NotificationEvent {
    idNotification: string; 
    idUser: string;        
    title: string;
    content: string;
    type: string;
    idRef: string;          
    isRead: boolean;
    createdAt: string;      
}