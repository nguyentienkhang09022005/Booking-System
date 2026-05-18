package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedNotebookStatus;
import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.NotebookResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.NoteBlockMapper;
import com.example.java_service.Mappers.NoteMapper;
import com.example.java_service.Mappers.NotebookMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.NoteBlockRepository;
import com.example.java_service.Repositories.Jpa.NoteRepository;
import com.example.java_service.Repositories.Jpa.NotebookRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotebookService {

    NotebookRepository notebookRepository;
    NoteRepository noteRepository;
    NoteBlockRepository noteBlockRepository;
    UserRepository userRepository;
    ImagesRepository imagesRepository;
    NotebookMapper notebookMapper;
    NoteMapper noteMapper;
    NoteBlockMapper noteBlockMapper;

    // ==================== NOTEBOOK ====================

    @Transactional
    public NotebookResponse createNotebook(NotebookCreationRequest request) {
        validateUserId(request.getIdUser());
        validateNotebookTitle(request.getTitle());
        validateTemplate(request.getTemplate());

        Users user = userRepository.findById(request.getIdUser())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Notebook notebook = notebookMapper.toEntity(request);
        notebook.setUsers(user);
        notebook.setIsFavorite(false);
        notebook.setCreatedAt(LocalDateTime.now());
        notebook.setUpdatedAt(LocalDateTime.now());

        Notebook saved = notebookRepository.save(notebook);

        if (request.getCoverImage() != null && !request.getCoverImage().trim().isEmpty()) {
            saveNotebookImage(saved.getIdNotebook(), request.getCoverImage());
        }

        return buildNotebookResponse(saved);
    }

    public List<NotebookResponse> getNotebooksByUser(UUID userId) {
        validateUserId(userId);
        List<Notebook> notebooks = notebookRepository.findByUserIdWithUser(userId);
        return buildNotebookResponseList(notebooks);
    }

    public NotebookResponse getNotebookDetail(UUID notebookId, UUID userId) {
        Notebook notebook = notebookRepository.findByIdWithNotes(notebookId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTEBOOK_NOT_FOUND));

        validateNotebookAccess(notebook, userId);
        return buildNotebookDetailResponse(notebook);
    }

    @Transactional
    public NotebookResponse updateNotebook(UUID notebookId, NotebookUpdateRequest request) {
        validateUserId(request.getIdUser());
        if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
            throw new ApiException(ErrorCode.NOTEBOOK_TITLE_REQUIRED);
        }

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTEBOOK_NOT_FOUND));
        validateNotebookAccess(notebook, request.getIdUser());

        notebookMapper.updateEntityFromRequest(request, notebook);

        if (request.getCoverImage() != null) {
            updateNotebookImage(notebookId, request.getCoverImage());
        }

        Notebook saved = notebookRepository.save(notebook);
        return buildNotebookResponse(saved);
    }

    @Transactional
    public void deleteNotebook(UUID notebookId, UUID userId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTEBOOK_NOT_FOUND));
        validateNotebookAccess(notebook, userId);

        notebookRepository.delete(notebook);
        imagesRepository.deleteByRefId(notebookId.toString());
    }

    // ==================== NOTE ====================

    @Transactional
    public NotebookResponse.NoteDetail createNote(UUID notebookId, NoteCreationRequest request) {
        validateUserId(request.getIdUser());
        validateNoteTitle(request.getTitle());

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTEBOOK_NOT_FOUND));
        validateNotebookAccess(notebook, request.getIdUser());

        Note note = noteMapper.toEntity(request);
        note.setNotebook(notebook);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        if (request.getDisplayOrder() == null) {
            long count = noteRepository.countByNotebookId(notebookId);
            note.setDisplayOrder((int) count);
        }

        Note saved = noteRepository.save(note);
        return buildNoteDetail(saved);
    }

    @Transactional
    public NotebookResponse.NoteDetail updateNote(UUID notebookId, UUID noteId, NoteUpdateRequest request) {
        validateUserId(request.getIdUser());
        if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
            throw new ApiException(ErrorCode.NOTE_TITLE_REQUIRED);
        }

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTE_NOT_FOUND));

        if (!note.getNotebook().getIdNotebook().equals(notebookId)) {
            throw new ApiException(ErrorCode.NOTE_NOT_FOUND);
        }
        validateNotebookAccess(note.getNotebook(), request.getIdUser());

        noteMapper.updateEntityFromRequest(request, note);
        note.setUpdatedAt(LocalDateTime.now());
        Note saved = noteRepository.save(note);
        return buildNoteDetail(saved);
    }

    @Transactional
    public void deleteNote(UUID notebookId, UUID noteId, UUID userId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTE_NOT_FOUND));

        if (!note.getNotebook().getIdNotebook().equals(notebookId)) {
            throw new ApiException(ErrorCode.NOTE_NOT_FOUND);
        }
        validateNotebookAccess(note.getNotebook(), userId);

        noteRepository.delete(note);
    }

    @Transactional
    public void reorderNotes(UUID notebookId, NotesReorderRequest request) {
        validateUserId(request.getIdUser());

        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTEBOOK_NOT_FOUND));
        validateNotebookAccess(notebook, request.getIdUser());

        for (NotesReorderRequest.NoteOrder order : request.getNotes()) {
            noteRepository.updateDisplayOrder(order.getIdNote(), order.getDisplayOrder());
        }
    }

    // ==================== BLOCK ====================

    @Transactional
    public NotebookResponse.BlockDetail createBlock(UUID noteId, BlockCreationRequest request) {
        validateUserId(request.getIdUser());
        validateBlockType(request.getBlockType());

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTE_NOT_FOUND));
        validateNotebookAccess(note.getNotebook(), request.getIdUser());

        NoteBlock block = noteBlockMapper.toEntity(request);
        block.setNote(note);
        block.setCreatedAt(LocalDateTime.now());

        if (request.getDisplayOrder() == null) {
            long count = noteBlockRepository.countByNoteId(noteId);
            block.setDisplayOrder((int) count);
        }

        NoteBlock saved = noteBlockRepository.save(block);
        return buildBlockDetail(saved);
    }

    @Transactional
    public NotebookResponse.BlockDetail updateBlock(UUID blockId, BlockUpdateRequest request) {
        validateUserId(request.getIdUser());
        if (request.getBlockType() != null) {
            validateBlockType(request.getBlockType());
        }

        NoteBlock block = noteBlockRepository.findById(blockId)
                .orElseThrow(() -> new ApiException(ErrorCode.BLOCK_NOT_FOUND));
        validateNotebookAccess(block.getNote().getNotebook(), request.getIdUser());

        noteBlockMapper.updateEntityFromRequest(request, block);
        NoteBlock saved = noteBlockRepository.save(block);
        return buildBlockDetail(saved);
    }

    @Transactional
    public void deleteBlock(UUID blockId, UUID userId) {
        NoteBlock block = noteBlockRepository.findById(blockId)
                .orElseThrow(() -> new ApiException(ErrorCode.BLOCK_NOT_FOUND));
        validateNotebookAccess(block.getNote().getNotebook(), userId);

        noteBlockRepository.delete(block);
    }

    @Transactional
    public void reorderBlocks(UUID noteId, BlocksReorderRequest request) {
        validateUserId(request.getIdUser());

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTE_NOT_FOUND));
        validateNotebookAccess(note.getNotebook(), request.getIdUser());

        for (BlocksReorderRequest.BlockOrder order : request.getBlocks()) {
            noteBlockRepository.updateDisplayOrder(order.getIdBlock(), order.getDisplayOrder());
        }
    }

    // ==================== SEARCH ====================

    public List<NotebookResponse> searchNotebooks(UUID userId, String query) {
        validateUserId(userId);
        List<Notebook> notebooks = notebookRepository.findByUserIdWithUser(userId);

        return notebooks.stream()
                .filter(n -> matchesQuery(n, query))
                .map(this::buildNotebookResponse)
                .collect(Collectors.toList());
    }

    // ==================== VALIDATION ====================

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND, "ID người dùng không được để trống!");
        }
    }

    private void validateNotebookTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ApiException(ErrorCode.NOTEBOOK_TITLE_REQUIRED);
        }
    }

    private void validateNoteTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ApiException(ErrorCode.NOTE_TITLE_REQUIRED);
        }
    }

    private void validateTemplate(String template) {
        if (template == null) return;
        List<String> validTemplates = List.of(
                PredefinedNotebookStatus.TEMPLATE_CUSTOM,
                PredefinedNotebookStatus.TEMPLATE_TRIP_PLANNER,
                PredefinedNotebookStatus.TEMPLATE_PACKING_LIST,
                PredefinedNotebookStatus.TEMPLATE_EXPENSE
        );
        if (!validTemplates.contains(template)) {
            throw new ApiException(ErrorCode.NOTEBOOK_INVALID_TEMPLATE);
        }
    }

    private void validateBlockType(String blockType) {
        if (blockType == null) return;
        List<String> validTypes = List.of(
                PredefinedNotebookStatus.BLOCK_TYPE_TEXT,
                PredefinedNotebookStatus.BLOCK_TYPE_IMAGE,
                PredefinedNotebookStatus.BLOCK_TYPE_HEADING,
                PredefinedNotebookStatus.BLOCK_TYPE_LIST,
                PredefinedNotebookStatus.BLOCK_TYPE_CHECKLIST,
                PredefinedNotebookStatus.BLOCK_TYPE_QUOTE,
                PredefinedNotebookStatus.BLOCK_TYPE_LINK,
                PredefinedNotebookStatus.BLOCK_TYPE_TABLE
        );
        if (!validTypes.contains(blockType)) {
            throw new ApiException(ErrorCode.BLOCK_INVALID_TYPE);
        }
    }

    private void validateNotebookAccess(Notebook notebook, UUID userId) {
        if (!notebook.getUsers().getIdUser().equals(userId)) {
            throw new ApiException(ErrorCode.NOTEBOOK_ACCESS_DENIED);
        }
    }

    private boolean matchesQuery(Notebook notebook, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String lowerQuery = query.toLowerCase();
        return (notebook.getTitle() != null && notebook.getTitle().toLowerCase().contains(lowerQuery)) ||
               (notebook.getDescription() != null && notebook.getDescription().toLowerCase().contains(lowerQuery));
    }

    // ==================== RESPONSE BUILDERS ====================

    private NotebookResponse buildNotebookResponse(Notebook notebook) {
        return NotebookResponse.builder()
                .idNotebook(notebook.getIdNotebook())
                .title(notebook.getTitle())
                .description(notebook.getDescription())
                .coverImage(notebook.getCoverImage())
                .template(notebook.getTemplate())
                .isFavorite(notebook.getIsFavorite())
                .createdAt(notebook.getCreatedAt())
                .updatedAt(notebook.getUpdatedAt())
                .owner(buildOwnerInfo(notebook.getUsers()))
                .notes(List.of())
                .images(getNotebookImages(notebook.getIdNotebook()))
                .build();
    }

    private NotebookResponse buildNotebookDetailResponse(Notebook notebook) {
        List<NotebookResponse.NoteDetail> noteResponses = noteRepository.findByNotebookIdOrderByDisplayOrder(notebook.getIdNotebook())
                .stream()
                .map(this::buildNoteDetailWithBlocks)
                .collect(Collectors.toList());

        return NotebookResponse.builder()
                .idNotebook(notebook.getIdNotebook())
                .title(notebook.getTitle())
                .description(notebook.getDescription())
                .coverImage(notebook.getCoverImage())
                .template(notebook.getTemplate())
                .isFavorite(notebook.getIsFavorite())
                .createdAt(notebook.getCreatedAt())
                .updatedAt(notebook.getUpdatedAt())
                .owner(buildOwnerInfo(notebook.getUsers()))
                .notes(noteResponses)
                .images(getNotebookImages(notebook.getIdNotebook()))
                .build();
    }

    private List<NotebookResponse> buildNotebookResponseList(List<Notebook> notebooks) {
        if (notebooks.isEmpty()) return List.of();

        List<String> notebookIds = notebooks.stream()
                .map(n -> n.getIdNotebook().toString())
                .collect(Collectors.toList());

        List<Images> allImages = imagesRepository.findByRefIdIn(notebookIds);
        Map<String, List<Images>> imagesByNotebookId = allImages.stream()
                .collect(Collectors.groupingBy(Images::getRefId));

        return notebooks.stream()
                .map(n -> {
                    List<String> urls = imagesByNotebookId.getOrDefault(n.getIdNotebook().toString(), List.of())
                            .stream()
                            .map(Images::getUrl)
                            .collect(Collectors.toList());
                    return NotebookResponse.builder()
                            .idNotebook(n.getIdNotebook())
                            .title(n.getTitle())
                            .description(n.getDescription())
                            .coverImage(n.getCoverImage())
                            .template(n.getTemplate())
                            .isFavorite(n.getIsFavorite())
                            .createdAt(n.getCreatedAt())
                            .updatedAt(n.getUpdatedAt())
                            .owner(buildOwnerInfo(n.getUsers()))
                            .notes(List.of())
                            .images(urls)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private NotebookResponse.NoteDetail buildNoteDetail(Note note) {
        return NotebookResponse.NoteDetail.builder()
                .idNote(note.getIdNote())
                .title(note.getTitle())
                .displayOrder(note.getDisplayOrder())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .blocks(List.of())
                .build();
    }

    private NotebookResponse.NoteDetail buildNoteDetailWithBlocks(Note note) {
        List<NotebookResponse.BlockDetail> blockResponses = noteBlockRepository.findByNoteIdOrderByDisplayOrder(note.getIdNote())
                .stream()
                .map(this::buildBlockDetail)
                .collect(Collectors.toList());

        return NotebookResponse.NoteDetail.builder()
                .idNote(note.getIdNote())
                .title(note.getTitle())
                .displayOrder(note.getDisplayOrder())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .blocks(blockResponses)
                .build();
    }

    private NotebookResponse.BlockDetail buildBlockDetail(NoteBlock block) {
        List<String> images = getBlockImages(block.getIdBlock());
        return NotebookResponse.BlockDetail.builder()
                .idBlock(block.getIdBlock())
                .idNote(block.getNote().getIdNote())
                .blockType(block.getBlockType())
                .content(block.getContent())
                .metadata(block.getMetadata())
                .displayOrder(block.getDisplayOrder())
                .createdAt(block.getCreatedAt())
                .images(images)
                .build();
    }

    private NotebookResponse.OwnerInfo buildOwnerInfo(Users user) {
        return NotebookResponse.OwnerInfo.builder()
                .idUser(user.getIdUser())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }

    // ==================== IMAGE HELPERS ====================

    private List<String> getNotebookImages(UUID notebookId) {
        return imagesRepository.findByRefId(notebookId.toString())
                .stream()
                .map(Images::getUrl)
                .collect(Collectors.toList());
    }

    private List<String> getBlockImages(UUID blockId) {
        return imagesRepository.findByRefId(blockId.toString())
                .stream()
                .map(Images::getUrl)
                .collect(Collectors.toList());
    }

    private void saveNotebookImage(UUID notebookId, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return;

        Images image = new Images();
        image.setUrl(imageUrl);
        image.setRefId(notebookId.toString());
        image.setType("notebook");
        image.setUploadedAt(LocalDateTime.now());
        imagesRepository.save(image);
    }

    private void updateNotebookImage(UUID notebookId, String imageUrl) {
        imagesRepository.deleteByRefId(notebookId.toString());
        saveNotebookImage(notebookId, imageUrl);
    }
}