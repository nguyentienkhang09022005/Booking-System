package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.BulletinBoardCreationRequest;
import com.example.java_service.Dto.Request.BulletinBoardUpdateRequest;
import com.example.java_service.Dto.Response.BulletinBoardResponse;
import com.example.java_service.Models.BulletinBoard;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BulletinBoardMapper {

    @Mapping(target = "idBulletin", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isApproved", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    BulletinBoard toEntity(BulletinBoardCreationRequest request);

    @Mapping(target = "author", source = "users")
    BulletinBoardResponse toResponse(BulletinBoard entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idBulletin", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isApproved", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isFeatured", ignore = true)
    void updateEntityFromRequest(BulletinBoardUpdateRequest request, @MappingTarget BulletinBoard entity);
}