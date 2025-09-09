package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Constant.PredefinedRole;
import com.example.Booking_Hotel_BE.Constant.PredefinedTypeUser;
import com.example.Booking_Hotel_BE.Constant.TypeImageConstant;
import com.example.Booking_Hotel_BE.Dto.Request.OtpRequest;
import com.example.Booking_Hotel_BE.Dto.Request.RegisterRequest;
import com.example.Booking_Hotel_BE.Exception.ApiException;
import com.example.Booking_Hotel_BE.Exception.ErrorCode;
import com.example.Booking_Hotel_BE.Mappers.UserMapper;
import com.example.Booking_Hotel_BE.Models.Images;
import com.example.Booking_Hotel_BE.Models.Roles;
import com.example.Booking_Hotel_BE.Models.TypeUser;
import com.example.Booking_Hotel_BE.Models.Users;
import com.example.Booking_Hotel_BE.Repositories.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegisterService
{
    UserRepository userRepository;
    RoleRepository roleRepository;
    TypeUserRepository typeUserRepository;
    ImageRepository imageRepository;
    OtpRepository otpRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    OtpService otpService;

    // Hàm đăng ký lưu người dùng xuống cơ sở dữ liệu
    public ResponseEntity<String> register(RegisterRequest request) {
        Optional<Users> userOpt = userRepository.findByEmail(request.getEmail());

        // Nếu user đã tồn tại
        if (userOpt.isPresent()) {
            Users user = userOpt.get();

            if (user.getPasswordUser() != null) {
                throw new ApiException(ErrorCode.USER_EXISTS);
            }

            // Nếu login google rồi thì chỉ bổ sung password
            validatePasswordAndOtp(request);

            user.setPasswordUser(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            return ResponseEntity.ok("Bổ sung mật khẩu cho tài khoản Google thành công!");
        }

        // Chưa có user thì tạo mới
        validatePasswordAndOtp(request);

        Users newUser = userMapper.toUser(request);
        newUser.setPasswordUser(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(getDefaultRole());
        newUser.setTypeUser(getDefaultTypeUser());

        userRepository.save(newUser);
        return ResponseEntity.ok("Đăng ký thành công!");
    }

    private void validatePasswordAndOtp(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (!otpRepository.existsById(request.getOtp())) {
            throw new ApiException(ErrorCode.OTP_NOT_FOUND);
        }

        ResponseEntity<String> verifyResult = otpService.verifyRegisterOtp(
                new OtpRequest(request.getOtp(), request.getEmail())
        );
        if (!verifyResult.getStatusCode().is2xxSuccessful()) {
            throw new ApiException(ErrorCode.OTP_INVALID);
        }
    }

    private Roles getDefaultRole() {
        return roleRepository.findById(PredefinedRole.USER_ROLE)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));
    }

    private TypeUser getDefaultTypeUser() {
        return typeUserRepository.findById(PredefinedTypeUser.TYPE_USER_DEFAULT)
                .orElseThrow(() -> new ApiException(ErrorCode.TYPE_USER_NOT_FOUND));
    }


    // Hàm lưu người dùng đăng nhập bằng Google xuống csdl
    @Transactional
    public Users processOAuth2User(String email, String firstName, String lastName, String urlAvatar)
    {
        Optional<Users> user = userRepository.findByEmail(email);

        if (user.isPresent())
            return user.get();

        Users newUser = new Users();
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);

        Roles role = roleRepository.findById(PredefinedRole.USER_ROLE)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));
        newUser.setRole(role);

        Users savedUser = userRepository.save(newUser);

        // Lưu ảnh đại diện
        Images image = new Images();
        image.setRefId(newUser.getIdUser().toString());
        image.setUrl(urlAvatar);
        image.setType(TypeImageConstant.USER_TYPE);
        image.setUploadedAt(LocalDateTime.now());
        imageRepository.save(image);
        return savedUser;
    }
}

