-- Script tạo bảng lưu vector cho RAG (PostgreSQL + PgVector)
-- Chạy script này trong PostgreSQL của bạn

-- 1. Tạo extension vector (bạn đã chạy rồi, nhưng để lại đây cho complete)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Tạo bảng booking_knowledge_base
-- dimension 768 = kích thước vector của Google text-embedding-004
CREATE TABLE IF NOT EXISTS booking_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    embedding VECTOR(768) NOT NULL,
    metadata JSONB
);

-- 3. Tạo index cho việc tìm kiếm vector (HNSW index cho hiệu suất cao)
CREATE INDEX IF NOT EXISTS booking_knowledge_base_embedding_idx 
ON booking_knowledge_base 
USING hnsw (embedding vector_cosine_ops);

-- 4. (Tùy chọn) Tạo index cho content để tìm kiếm full-text
CREATE INDEX IF NOT EXISTS booking_knowledge_base_content_idx 
ON booking_knowledge_base 
USING gin (to_tsvector('vietnamese', content));
