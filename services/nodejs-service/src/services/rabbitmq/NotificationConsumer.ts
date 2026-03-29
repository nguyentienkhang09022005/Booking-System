import amqp from 'amqplib';
import { NotificationModel } from '../../collection/Notification.ts'; 
import { emitNotification } from '../../config/socket.ts'; 

const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://localhost';
const EXCHANGE_NAME = process.env.EXCHANGE_NAME || 'exchange-notification-1';
const QUEUE_NAME = process.env.QUEUE_NAME || 'queue-notification-1';
const ROUTING_KEY = process.env.ROUTING_KEY || 'key-notification-1';

export const startNotificationConsumer = async () => {
  try {
    const connection = await amqp.connect(RABBITMQ_URL);
    const channel = await connection.createChannel();

    await channel.assertExchange(EXCHANGE_NAME, 'direct', { durable: true });
    await channel.assertQueue(QUEUE_NAME, { durable: true });
    
    await channel.bindQueue(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

    console.log(`🐰 RabbitMQ Consumer đã sẵn sàng. Đang chờ tin nhắn tại queue: ${QUEUE_NAME}`);

    channel.consume(QUEUE_NAME, async (msg) => {
      if (!msg) return;

      try {
        const rawContent = msg.content.toString();
        const eventData: any = JSON.parse(rawContent);
        
        console.log('📩 Nhận thông báo từ Java:', eventData);

        const newNotification = new NotificationModel({
            idNotification: eventData.idNotification,
            idUser: eventData.idUser, 
            title: eventData.title,
            content: eventData.content,
            type: eventData.type,
            refId: eventData.idRef,
            isRead: eventData.read ?? false,            
            createdAt: new Date(eventData.createdAt)
        });

        await newNotification.save();
        console.log('✅ Đã lưu notification vào DB');

        if (emitNotification) { 
            emitNotification(newNotification.idUser, {
                idNotification: newNotification.idNotification,
                title: newNotification.title,
                content: newNotification.content ?? "",
                type: newNotification.type,
                refId: newNotification.refId ?? null,
                createdAt: newNotification.createdAt,
            });
            console.log('📡 Đã bắn socket tới user:', newNotification.idUser);
        }

        channel.ack(msg);

      } catch (error) {
        console.error('❌ Lỗi xử lý tin nhắn RabbitMQ:', error);
        channel.nack(msg, false, false); 
      }
    });

  } catch (error) {
    console.error('❌ Không thể kết nối RabbitMQ:', error);
    setTimeout(startNotificationConsumer, 5000);
  }
};