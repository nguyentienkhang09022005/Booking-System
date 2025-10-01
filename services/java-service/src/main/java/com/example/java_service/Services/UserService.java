package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedRole;
import com.example.java_service.Constant.PredefinedTypeUser;
import com.example.java_service.Dto.Request.UserCreationRequest;
import com.example.java_service.Dto.Request.UserUpdateRequest;
import com.example.java_service.Dto.Response.UserResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.UserMapper;
import com.example.java_service.Models.*;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public List<UserResponse> getListUsers()
    {
        List<Users> usersList = userRepository.findAll();

        if (usersList.isEmpty()) {
            return List.of();
        }

        // Lấy ID của tất cả người dùng
        List<String> usersIds = usersList.stream()
                .map(hotel -> hotel.getIdUser().toString())
                .toList();

        List<Images> avatars = imagesRepository.findByRefIdIn(usersIds);

        // Map userId -> avatarUrl (mỗi user chỉ có 1 ảnh)
        Map<String, String> avatarMap = avatars.stream()
                .collect(Collectors.toMap(
                        Images::getRefId,
                        Images::getUrl,
                        (existing, replacement) -> replacement
                ));

        return usersList.stream()
                .map(user -> {
                    UserResponse response = userMapper.toUserResponse(user);
                    String hotelIdStr = user.getIdUser().toString();

                    response.setAvatar(avatarMap.get(hotelIdStr));
                    response.setTypeUser(user.getTypeUser());
                    response.setRole(user.getRole());
                    return response;
                }).toList();
    }

    // Hàm lấy thông tin chi tiết người dùng
    public UserResponse getInfUser(UUID idUser)
    {
        Users user = userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String avatarUrl = imagesRepository.findByRefId(user.getIdUser().toString())
                .stream()
                .map(Images::getUrl)
                .findFirst()
                .orElse(null);

        UserResponse response = userMapper.toUserResponse(user);
        response.setAvatar(avatarUrl);
        response.setTypeUser(user.getTypeUser());
        response.setRole(user.getRole());
        return response;
    }

    // Hàm thêm thông tin người dùng
    public UserResponse saveUser(UserCreationRequest request) {
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
        // Bắt buộc nhập sđt
        if (request.getPhone() == null) {
            throw new ApiException(ErrorCode.PHONE_REQUIRED);
        }

        // Kiểm tra kích thước số điện thoại
        if (request.getPhone().length() < MIN_LENGTH_OF_PHONE || request.getPhone().length() > MAX_LENGTH_OF_PHONE) {
            throw new ApiException(ErrorCode.INVALID_PHONE_LENGTH);
        }

        // Kiểm tra có chữ trong sđt không
        if (!request.getPhone().matches("\\d+")) {
            throw new ApiException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        // Kiểm tra role
        Roles role = roleRepository.findById(request.getRole()).orElseThrow(
                () -> new ApiException(ErrorCode.ROLE_NOT_FOUND)
        );
        Users newUser = userMapper.toUser(request);
        newUser.setPasswordUser(passwordEncoder.encode(request.getPasswordUser()));
        newUser.setTypeUser(getDefaultTypeUser());
        newUser.setRole(role);
        Users savedUser = userRepository.save(newUser);
        // Lưu ảnh từ URL đã upload
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            Images avatar = new Images();
            avatar.setUrl(request.getAvatar());
            avatar.setRefId(savedUser.getIdUser().toString());
            imagesRepository.save(avatar);
        }
        var userResponse = userMapper.toUserResponse(savedUser);
        userResponse.setRole(savedUser.getRole());
        userResponse.setTypeUser(getDefaultTypeUser());
        return userResponse;
    }

    // Hàm sửa thông tin người dùng
    public UserResponse updateUser(UserUpdateRequest request, UUID idUser)
    {
        Users user = userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (request.getPhone() != null && !request.getPhone().isEmpty())
        {
            if (request.getPhone().length() < MIN_LENGTH_OF_PHONE || request.getPhone().length() > MAX_LENGTH_OF_PHONE) {
                throw new ApiException(ErrorCode.INVALID_PHONE_LENGTH);
            }

            if (!request.getPhone().matches("\\d+")) {
                throw new ApiException(ErrorCode.INVALID_PHONE_FORMAT);
            }

            userRepository.findByPhone(request.getPhone()).ifPresent(existing -> {
                if (!existing.getIdUser().equals(idUser)) {
                    throw new ApiException(ErrorCode.PHONE_EXISTS);
                }
            });
            user.setPhone(request.getPhone());
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty())
        {
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getIdUser().equals(idUser)) {
                    throw new ApiException(ErrorCode.EMAIL_EXISTS);
                }
            });
            user.setEmail(request.getEmail());
        }

        userMapper.updateUserFromRequest(request, user);
        user.setTypeUser(typeUserRepository.findById(request.getTypeUser()).orElseThrow(
                () -> new ApiException(ErrorCode.TYPE_USER_NOT_FOUND)
        ));
        user.setRole(roleRepository.findById(request.getRole()).orElseThrow(
                () -> new ApiException(ErrorCode.ROLE_NOT_FOUND)
        ));
        Users updateUser = userRepository.save(user);

        // Nếu có upload ảnh mới
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            imagesRepository.deleteByRefId(updateUser.getIdUser().toString());

            Images avatar = new Images();
                avatar.setUrl(request.getAvatar());
                avatar.setRefId(updateUser.getIdUser().toString());
            imagesRepository.save(avatar);
        }
        var userResponse = userMapper.toUserResponse(updateUser);
        userResponse.setRole(updateUser.getRole());
        userResponse.setTypeUser(updateUser.getTypeUser());
        return userResponse;
    }

    // Hàm xóa người dùng
    public ResponseEntity<String> deleteUser(UUID idUser)
    {
        Users user = userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );
        userRepository.delete(user);
        imagesRepository.deleteByRefId(idUser.toString());
        return ResponseEntity.ok("Xóa người dùng thành công!");
    }
}
