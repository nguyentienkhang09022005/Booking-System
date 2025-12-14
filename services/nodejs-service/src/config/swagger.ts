import swaggerJsdoc from "swagger-jsdoc";
import dotenv from "dotenv";
dotenv.config();

const serverUrl = process.env.SERVER_URL || "/";
const options: swaggerJsdoc.Options = {
  definition: {
    openapi: "3.0.0",
    info: {
      title: "Node.js Chat Service API",
      version: "1.0.0",
      description: "API documentation for the Chat Service",
    },
    servers: [
      {
        url: serverUrl, 
        description: "Server",
      },
      {
        url: "http://localhost:3000",
        description: "Local",
      },
    ],
    components: {
      schemas: {
        Receiver: {
          type: "object",
          properties: {
            firstname: {
              type: "string",
              description: "First name of the receiver",
            },
            lastname: {
              type: "string",
              description: "Last name of the receiver",
            },
            email: {
              type: "string",
              description: "Email of the receiver",
            },
          },
        },
        Message: {
          type: "object",
          properties: {
            idMessage: {
              type: "string",
              description: "Message ID",
            },
            idSender: {
              type: "string",
              description: "ID of the sender",
            },
            idReceiver: {
              type: "string",
              description: "ID of the receiver",
            },
            message: {
              type: "string",
              description: "Message content",
            },
            type: {
              type: "string",
              enum: ["text", "image", "file"],
              description: "Message type",
            },
            status: {
              type: "string",
              enum: ["sent", "seen"],
              description: "Message status",
            },
            createdAt: {
              type: "string",
              format: "date-time",
              description: "Creation date",
            },
          },
        },
        SendMessageRequest: {
          type: "object",
          required: ["idSender", "idReceiver", "message"],
          properties: {
            idSender: {
              type: "string",
              description: "ID of the sender",
            },
            idReceiver: {
              type: "string",
              description: "ID of the receiver",
            },
            message: {
              type: "string",
              description: "Message content",
            },
            type: {
              type: "string",
              enum: ["text", "image", "file"],
              description: "Message type (default: text)",
            },
          },
        },
        Error: {
          type: "object",
          properties: {
            error: {
              type: "string",
              description: "Error message",
            },
          },
        },
        Notification: {
          type: "object",
          properties: {
            idNotification: {
              type: "string",
              description: "Notification ID",
            },
            idUser: {
              type: "string",
              description: "User ID",
            },
            title: {
              type: "string",
              description: "Notification title",
            },
            content: {
              type: "string",
              description: "Notification content",
            },
            type: {
              type: "string",
              enum: ["booking", "message", "evaluate", "favorite", "system"],
              description: "Notification type",
            },
            refId: {
              type: "string",
              nullable: true,
              description: "Reference ID",
            },
            isRead: {
              type: "boolean",
              description: "Read status",
            },
            createdAt: {
              type: "string",
              format: "date-time",
              description: "Creation date",
            },
          },
        },
        SuccessMessage: {
          type: "object",
          properties: {
            message: {
              type: "string",
              description: "Success message",
            },
          },
        },
      },
    },
  },
  apis: ["./src/routes/*.ts", "./dist/routes/*.js"],
};

const swaggerSpec = swaggerJsdoc(options);

export default swaggerSpec;
