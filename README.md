🚀 BookingNext Backend (Microservices)
📌 Overview

BookingNext Backend là hệ thống backend được xây dựng theo kiến trúc Microservices đa ngôn ngữ, phục vụ nền tảng đặt phòng hiện đại tích hợp tài chính số.

Hệ thống được thiết kế để:

Đảm bảo hiệu năng cao, khả năng mở rộng

Giải quyết triệt để vấn đề overbooking

Hỗ trợ xử lý thời gian thực, thanh toán điện tử, AI tư vấn

🏗️ System Architecture
Microservices Architecture
Client (Web / Mobile)
        |
        v
   API Gateway
        |
-------------------------------------------------
|        |             |                       |
Auth    Core Service   Finance & Report        Real-time & AI
(Java)  (Java)         (Golang)                (Node.js)
-------------------------------------------------
        |
Data & Infrastructure Layer
(PostgreSQL, MongoDB, Redis, RabbitMQ)
Architectural Highlights

Microservices & Polyglot Backend

Event-driven với RabbitMQ

Real-time Communication (WebSocket)

Secure Authentication (JWT, OAuth2)

Containerized with Docker

🔧 Technology Stack
Backend Services
Service	Technology
API Gateway	Spring Cloud Gateway
Authentication	Java Spring Boot + Spring Security
Core Business	Java Spring Boot
Finance & Report	Golang + Gin
Real-time & AI	Node.js + Express + Socket.IO
Data & Infrastructure
Component	Technology
Relational DB	PostgreSQL
NoSQL DB	MongoDB
Cache	Redis
Message Broker	RabbitMQ
Containerization	Docker, Docker Compose
External Integrations

ZaloPay Payment Gateway

Google OAuth2

Google Gemini AI

Cloudinary (Image Storage)

SendGrid (Email Service)

🧩 Microservices Description
1️⃣ API Gateway Service (Java – Spring Cloud Gateway)

Role:
Central entry point cho toàn bộ hệ thống.

Responsibilities:

Route request đến các microservice

Validate JWT token

Global exception handling

Rate limiting & logging

2️⃣ Authentication Service (Java – Spring Boot)

Role:
Quản lý xác thực & phân quyền người dùng.

Main Features:

Register / Login (Email + OTP)

Login with Google OAuth2

JWT Access Token & Refresh Token

Refresh Token lưu trong Redis

3️⃣ Core Business Service (Java – Spring Boot)

Role:
Xử lý nghiệp vụ cốt lõi của hệ thống.

Main Modules:

User Management

Hotel & Room Management

Booking Flow

Voucher & Review Management

Key Responsibilities:

Kiểm tra phòng trống real-time

Giữ chỗ (booking hold) chống overbooking

Phát sự kiện sang RabbitMQ

4️⃣ Finance & Report Service (Golang – Gin)

Role:
Xử lý tài chính & báo cáo hiệu năng cao.

Main Features:

Tích hợp ZaloPay

Xử lý callback / IPN

Tổng hợp doanh thu

Báo cáo thống kê real-time

Why Golang?

High performance

Concurrency mạnh

Độ trễ thấp cho giao dịch tài chính

5️⃣ Real-time & AI Service (Node.js – Express)

Role:
Xử lý các tác vụ thời gian thực & AI.

Main Features:

Real-time Notification (Socket.IO)

Chat User ↔ Hotel

AI Chatbot tư vấn du lịch

AI Content Moderation (Review)

🔁 Event-driven Communication

RabbitMQ được sử dụng để giao tiếp bất đồng bộ giữa các service.

Ví dụ Event Flow:
Booking Created
   |
   v
RabbitMQ
   |
-------------------------
|           |           |
Finance     Notification AI Service
Service     Service
➡️ Giúp hệ thống:

Giảm coupling

Tăng khả năng chịu tải

Dễ mở rộng

🔐 Security

JWT Authentication

Role-based Authorization (Admin / Company / User)

OAuth2 (Google Login)

Secure Payment Callback Validation

Sensitive data immutability (Payment, Booking)

🐳 Docker & Deployment
Run Backend with Docker Compose
docker-compose up --build


Services Included:

All backend microservices

PostgreSQL

MongoDB

Redis

RabbitMQ

📂 Project Structure (Backend)
bookingnext-backend/
│
├── api-gateway/
├── auth-service/
├── core-service/
├── finance-report-service/
├── realtime-ai-service/
│
├── docker-compose.yml
└── README.md
🎯 Key Features Summary

Microservices Architecture

Polyglot Backend (Java + Golang + Node.js)

Real-time Booking & Notification

Secure Payment Integration

AI-powered Travel Assistant

High Scalability & Maintainability
