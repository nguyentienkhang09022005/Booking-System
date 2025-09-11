package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedRole;
import com.example.java_service.Constant.PredefinedTypeUser;
import com.example.java_service.Dto.Request.UserCreationRequest;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.UserMapper;
import com.example.java_service.Models.Images;
import com.example.java_service.Models.Roles;
import com.example.java_service.Models.TypeUser;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.RoleRepository;
import com.example.java_service.Repositories.Jpa.TypeUserRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService
{
    UserRepository userRepository;
    TypeUserRepository typeUserRepository;
    RoleRepository roleRepository;
    ImagesRepository imagesRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    // Giới hạn số điện thoại
    static final int MAX_LENGTH_OF_PHONE = 12;
    static final int MIN_LENGTH_OF_PHONE = 10;

    // Hàm set role mặc định
    private Roles getDefaultRole() {
        return roleRepository.findById(PredefinedRole.USER_ROLE)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));
    }

    // Hàm set loại người dùng mặc định
    private TypeUser getDefaultTypeUser() {
        return typeUserRepository.findById(PredefinedTypeUser.TYPE_USER_DEFAULT)
                .orElseThrow(() -> new ApiException(ErrorCode.TYPE_USER_NOT_FOUND));
    }

    // Hàm lấy danh sách người dùng

    // Hàm lấy thông tin chi tiết người dùng

    // Hàm thêm thông tin người dùng
    public ResponseEntity<String> saveUser(UserCreationRequest request) {
        // Kiểm tra email hoặc phone đã tồn tại
        Optional<Users> existingUser = userRepository.findByEmailOrPhone(request.getEmail(), request.getPhone());
        if (existingUser.isPresent()) {
            if (existingUser.get().getEmail().equals(request.getEmail())) {
                throw new ApiException(ErrorCode.EMAIL_EXISTS);
            }
            if (existingUser.get().getPhone().equals(request.getPhone())) {
                throw new ApiException(ErrorCode.PHONE_EXISTS);
            }
        }
        // Kiểm tra kích thước số điện thoại
        if (request.getPhone().length() < MIN_LENGTH_OF_PHONE || request.getPhone().length() > MAX_LENGTH_OF_PHONE) {
            throw new ApiException(ErrorCode.INVALID_PHONE_LENGTH);
        }

        // Kiểm tra có chữ trong sđt không
        if (!request.getPhone().matches("\\d+")) {
            throw new ApiException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        Users newUser = userMapper.toUser(request);
        newUser.setPasswordUser(passwordEncoder.encode(request.getPasswordUser()));
        newUser.setTypeUser(getDefaultTypeUser());
        newUser.setRole(getDefaultRole());
        Users savedUser = userRepository.save(newUser);
        // Lưu ảnh từ URL đã upload
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            Images image = new Images();
            image.setUrl(request.getAvatar());
            image.setRefId(savedUser.getIdUser().toString());
            imagesRepository.save(image);
        }
        return ResponseEntity.ok().body("Tạo người dùng thành công");
    }

        // Hàm sửa thông tin người dùng
        // Hàm xóa người dùng
}
