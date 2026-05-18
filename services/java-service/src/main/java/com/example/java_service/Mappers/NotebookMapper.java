package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.NotebookCreationRequest;
import com.example.java_service.Dto.Request.NotebookUpdateRequest;
import com.example.java_service.Models.Notebook;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NotebookMapper {

    @Mapping(target = "idNotebook", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "isFavorite", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Notebook toEntity(NotebookCreationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idNotebook", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(NotebookUpdateRequest request, @MappingTarget Notebook entity);
}