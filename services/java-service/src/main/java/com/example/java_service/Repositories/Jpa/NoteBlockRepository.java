package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.NoteBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteBlockRepository extends JpaRepository<NoteBlock, UUID> {

    @Query("SELECT b FROM NoteBlock b WHERE b.note.idNote = :noteId ORDER BY b.displayOrder ASC")
    List<NoteBlock> findByNoteIdOrderByDisplayOrder(@Param("noteId") UUID noteId);

    @Modifying
    @Query("UPDATE NoteBlock b SET b.displayOrder = :order WHERE b.idBlock = :id")
    void updateDisplayOrder(@Param("id") UUID id, @Param("order") int order);

    @Query("SELECT COUNT(b) FROM NoteBlock b WHERE b.note.idNote = :noteId")
    long countByNoteId(@Param("noteId") UUID noteId);
}