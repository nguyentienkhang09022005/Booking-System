package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Notebook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotebookRepository extends JpaRepository<Notebook, UUID> {

    @Query("SELECT n FROM Notebook n JOIN FETCH n.users WHERE n.idNotebook = :id")
    Optional<Notebook> findByIdWithUser(@Param("id") UUID id);

    @Query("SELECT n FROM Notebook n JOIN FETCH n.users WHERE n.users.idUser = :userId ORDER BY n.createdAt DESC")
    List<Notebook> findByUserIdWithUser(@Param("userId") UUID userId);

    @Query("SELECT n FROM Notebook n JOIN FETCH n.users WHERE n.users.idUser = :userId ORDER BY n.createdAt DESC")
    Page<Notebook> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT n FROM Notebook n LEFT JOIN FETCH n.notes WHERE n.idNotebook = :id")
    Optional<Notebook> findByIdWithNotes(@Param("id") UUID id);

    @Query("SELECT COUNT(n) FROM Notebook n WHERE n.users.idUser = :userId")
    long countByUserId(@Param("userId") UUID userId);

    boolean existsByIdNotebookAndUsersIdUser(UUID idNotebook, UUID userId);
}