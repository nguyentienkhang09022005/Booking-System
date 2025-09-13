package com.example.java_service.Services;

import com.example.java_service.Dto.Request.RoleRequest;
import com.example.java_service.Dto.Response.RoleResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.RoleMapper;
import com.example.java_service.Models.Roles;
import com.example.java_service.Repositories.Jpa.RoleRepository;
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
public class RoleService {

    RoleRepository roleRepository;
    RoleMapper roleMapper;

    // Lấy danh sách role
    public List<Roles> getAllRoles()
    {
        return roleRepository.findAll();
    }

    // Tạo role mới
    public RoleResponse saveRole(RoleRequest request) {
        if (roleRepository.findById(request.getRoleName()).isPresent())
        {
            throw new ApiException(ErrorCode.ROLE_EXISTS);
        }
        Roles role = roleRepository.save(roleMapper.toRole(request));
        return roleMapper.toRoleResponse(role);
    }

    // Xóa role
    public ResponseEntity<String> deleteRole(String roleName)
    {
        Roles role = roleRepository.findById(roleName).orElseThrow(
                () -> new ApiException(ErrorCode.ROLE_NOT_FOUND)
        );
        roleRepository.delete(role);
        return ResponseEntity.ok("Xóa role thành công!");
    }
}
