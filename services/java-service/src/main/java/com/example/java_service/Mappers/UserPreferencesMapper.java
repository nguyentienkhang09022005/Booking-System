package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.UserPreferencesRequest;
import com.example.java_service.Dto.Response.UserPreferencesResponse;
import com.example.java_service.Models.UserPreferences;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserPreferencesMapper {

    @Mapping(target = "idPreference", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserPreferences toUserPreferences(UserPreferencesRequest request);

    @Mapping(target = "idPreference", source = "idPreference")
    @Mapping(target = "idUser", source = "users.idUser")
    UserPreferencesResponse toResponse(UserPreferences preferences);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idPreference", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(UserPreferencesRequest request, @MappingTarget UserPreferences preferences);
}