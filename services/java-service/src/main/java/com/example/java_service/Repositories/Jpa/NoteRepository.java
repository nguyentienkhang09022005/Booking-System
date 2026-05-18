package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.blocks WHERE n.idNote = :id")
    Optional<Note> findByIdWithBlocks(@Param("id") UUID id);

    @Query("SELECT n FROM Note n WHERE n.notebook.idNotebook = :notebookId ORDER BY n.displayOrder ASC")
    List<Note> findByNotebookIdOrderByDisplayOrder(@Param("notebookId") UUID notebookId);

    @Query("SELECT n FROM Note n WHERE n.notebook.idNotebook = :notebookId ORDER BY n.displayOrder ASC")
    Page<Note> findByNotebookId(@Param("notebookId") UUID notebookId, Pageable pageable);

    @Modifying
    @Query("UPDATE Note n SET n.displayOrder = :order WHERE n.idNote = :id")
    void updateDisplayOrder(@Param("id") UUID id, @Param("order") int order);

    @Query("SELECT COUNT(n) FROM Note n WHERE n.notebook.idNotebook = :notebookId")
    long countByNotebookId(@Param("notebookId") UUID notebookId);
}