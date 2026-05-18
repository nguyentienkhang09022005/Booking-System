package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.NotebookResponse;
import com.example.java_service.Services.NotebookService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/notebook")
public class NotebookController {

    NotebookService notebookService;

    // ==================== NOTEBOOK ENDPOINTS ====================

    @PostMapping
    public ApiResponse<NotebookResponse> createNotebook(@RequestBody NotebookCreationRequest request) {
        return ApiResponse.<NotebookResponse>builder()
                .message("Tạo sổ tay thành công!")
                .data(notebookService.createNotebook(request))
                .build();
    }

    @GetMapping("/list")
    public ApiResponse<List<NotebookResponse>> getNotebooks(@RequestParam UUID idUser) {
        return ApiResponse.<List<NotebookResponse>>builder()
                .message("Lấy danh sách sổ tay thành công!")
                .data(notebookService.getNotebooksByUser(idUser))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<NotebookResponse> getNotebookDetail(
            @PathVariable UUID id,
            @RequestParam UUID idUser) {
        return ApiResponse.<NotebookResponse>builder()
                .message("Lấy chi tiết sổ tay thành công!")
                .data(notebookService.getNotebookDetail(id, idUser))
                .build();
    }

    @PatchMapping("/{id}")
    public ApiResponse<NotebookResponse> updateNotebook(
            @PathVariable UUID id,
            @RequestBody NotebookUpdateRequest request) {
        return ApiResponse.<NotebookResponse>builder()
                .message("Cập nhật sổ tay thành công!")
                .data(notebookService.updateNotebook(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotebook(
            @PathVariable UUID id,
            @RequestParam UUID idUser) {
        notebookService.deleteNotebook(id, idUser);
        return ApiResponse.<Void>builder()
                .message("Xóa sổ tay thành công!")
                .build();
    }

    // ==================== NOTE ENDPOINTS ====================

    @PostMapping("/{id}/note")
    public ApiResponse<NotebookResponse.NoteDetail> createNote(
            @PathVariable UUID id,
            @RequestBody NoteCreationRequest request) {
        return ApiResponse.<NotebookResponse.NoteDetail>builder()
                .message("Tạo trang thành công!")
                .data(notebookService.createNote(id, request))
                .build();
    }

    @PatchMapping("/{noteId}/note/{id}")
    public ApiResponse<NotebookResponse.NoteDetail> updateNote(
            @PathVariable UUID noteId,
            @PathVariable UUID id,
            @RequestBody NoteUpdateRequest request) {
        return ApiResponse.<NotebookResponse.NoteDetail>builder()
                .message("Cập nhật trang thành công!")
                .data(notebookService.updateNote(noteId, id, request))
                .build();
    }

    @DeleteMapping("/{notebookId}/note/{id}")
    public ApiResponse<Void> deleteNote(
            @PathVariable UUID notebookId,
            @PathVariable UUID id,
            @RequestParam UUID idUser) {
        notebookService.deleteNote(notebookId, id, idUser);
        return ApiResponse.<Void>builder()
                .message("Xóa trang thành công!")
                .build();
    }

    @PatchMapping("/{id}/reorder")
    public ApiResponse<Void> reorderNotes(
            @PathVariable UUID id,
            @RequestBody NotesReorderRequest request) {
        notebookService.reorderNotes(id, request);
        return ApiResponse.<Void>builder()
                .message("Sắp xếp lại trang thành công!")
                .build();
    }

    // ==================== BLOCK ENDPOINTS ====================

    @PostMapping("/{noteId}/block")
    public ApiResponse<NotebookResponse.BlockDetail> createBlock(
            @PathVariable UUID noteId,
            @RequestBody BlockCreationRequest request) {
        return ApiResponse.<NotebookResponse.BlockDetail>builder()
                .message("Tạo block thành công!")
                .data(notebookService.createBlock(noteId, request))
                .build();
    }

    @PatchMapping("/block/{id}")
    public ApiResponse<NotebookResponse.BlockDetail> updateBlock(
            @PathVariable UUID id,
            @RequestBody BlockUpdateRequest request) {
        return ApiResponse.<NotebookResponse.BlockDetail>builder()
                .message("Cập nhật block thành công!")
                .data(notebookService.updateBlock(id, request))
                .build();
    }

    @DeleteMapping("/block/{id}")
    public ApiResponse<Void> deleteBlock(
            @PathVariable UUID id,
            @RequestParam UUID idUser) {
        notebookService.deleteBlock(id, idUser);
        return ApiResponse.<Void>builder()
                .message("Xóa block thành công!")
                .build();
    }

    @PatchMapping("/{noteId}/reorder-blocks")
    public ApiResponse<Void> reorderBlocks(
            @PathVariable UUID noteId,
            @RequestBody BlocksReorderRequest request) {
        notebookService.reorderBlocks(noteId, request);
        return ApiResponse.<Void>builder()
                .message("Sắp xếp lại block thành công!")
                .build();
    }

    // ==================== SEARCH ====================

    @GetMapping("/search")
    public ApiResponse<List<NotebookResponse>> searchNotebooks(
            @RequestParam UUID idUser,
            @RequestParam(required = false) String query) {
        return ApiResponse.<List<NotebookResponse>>builder()
                .message("Tìm kiếm sổ tay thành công!")
                .data(notebookService.searchNotebooks(idUser, query))
                .build();
    }
}