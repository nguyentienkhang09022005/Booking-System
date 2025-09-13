package com.example.java_service.Services;

import com.example.java_service.Dto.Request.TypeUserRequest;
import com.example.java_service.Dto.Response.TypeUserResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.TypeUserMapper;
import com.example.java_service.Models.TypeUser;
import com.example.java_service.Repositories.Jpa.TypeUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TypeUserService
{
    TypeUserRepository typeUserRepository;
    TypeUserMapper typeUserMapper;

    // Hàm lấy danh sách loại người dùng
    public List<TypeUser> getAllTypeUsers()
    {
        return typeUserRepository.findAll();
    }

    // Hàm tạo loại người dùng mới
    public TypeUserResponse saveTypeUser(TypeUserRequest request)
    {
        if (typeUserRepository.findById(request.getTypeUser()).isPresent()) {
            throw new ApiException(ErrorCode.TYPE_USER_EXISTS);
        }
        TypeUser typeUser = typeUserRepository.save(typeUserMapper.toTypeUser(request));
        return typeUserMapper.toTypeUserResponse(typeUser);

    }

    // Hàm xóa loại người dùng
    public ResponseEntity<String> deleteTypeUser(String typeUserName)
    {
        TypeUser typeUser = typeUserRepository.findById(typeUserName).orElseThrow(
                () -> new ApiException(ErrorCode.TYPE_USER_NOT_FOUND)
        );
        typeUserRepository.delete(typeUser);
        return ResponseEntity.ok("Xóa loại người dùng thành công!");
    }

}
