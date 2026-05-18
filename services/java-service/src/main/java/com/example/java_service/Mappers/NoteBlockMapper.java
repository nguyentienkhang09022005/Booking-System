package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.BlockCreationRequest;
import com.example.java_service.Dto.Request.BlockUpdateRequest;
import com.example.java_service.Models.NoteBlock;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NoteBlockMapper {

    @Mapping(target = "idBlock", ignore = true)
    @Mapping(target = "note", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    NoteBlock toEntity(BlockCreationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idBlock", ignore = true)
    @Mapping(target = "note", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(BlockUpdateRequest request, @MappingTarget NoteBlock entity);
}