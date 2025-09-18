package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.FavoriteRequest;
import com.example.java_service.Models.Favorite;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FavoriteMapper
{
    @Mapping(target = "createdAt", ignore = true)
    Favorite toFavorite(FavoriteRequest request);
}
