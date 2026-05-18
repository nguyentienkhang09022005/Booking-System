package com.example.java_service.Models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "note_block")
public class NoteBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_block", nullable = false, updatable = false)
    private UUID idBlock;

    @ManyToOne
    @JoinColumn(name = "id_note", nullable = false, foreignKey = @ForeignKey(name = "FK_BLOCK_NOTE"))
    private Note note;

    @Column(name = "block_type", nullable = false, length = 20)
    private String blockType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}