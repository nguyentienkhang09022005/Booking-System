package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.NoteCreationRequest;
import com.example.java_service.Dto.Request.NoteUpdateRequest;
import com.example.java_service.Models.Note;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NoteMapper {

    @Mapping(target = "idNote", ignore = true)
    @Mapping(target = "notebook", ignore = true)
    @Mapping(target = "blocks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Note toEntity(NoteCreationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idNote", ignore = true)
    @Mapping(target = "notebook", ignore = true)
    @Mapping(target = "blocks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(NoteUpdateRequest request, @MappingTarget Note entity);
}