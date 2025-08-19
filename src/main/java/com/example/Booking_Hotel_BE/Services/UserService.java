package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Constant.PredefinedRole;
import com.example.Booking_Hotel_BE.Constant.PredefinedTypeUser;
import com.example.Booking_Hotel_BE.Dto.Request.RegisterRequest;
import com.example.Booking_Hotel_BE.Exception.ApiException;
import com.example.Booking_Hotel_BE.Exception.ErrorCode;
import com.example.Booking_Hotel_BE.Mappers.UserMapper;
import com.example.Booking_Hotel_BE.Models.Roles;
import com.example.Booking_Hotel_BE.Models.TypeUser;
import com.example.Booking_Hotel_BE.Models.Users;
import com.example.Booking_Hotel_BE.Repositories.OtpRepository;
import com.example.Booking_Hotel_BE.Repositories.RoleRepository;
import com.example.Booking_Hotel_BE.Repositories.TypeUserRepository;
import com.example.Booking_Hotel_BE.Repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService
{
    UserRepository userRepository;
    RoleRepository roleRepository;
    TypeUserRepository typeUserRepository;
    OtpRepository otpRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    // Hàm lấy danh sách người dùng
    public List<Users> findAll()
    {
        return userRepository.findAll();
    }

    // Hàm đăng ký lưu người dùng xuống cơ sở dữ liệu
    public void register(RegisterRequest request)
    {
        if (!request.getPassword().equals(request.getConfirmPassword()))
            throw  new ApiException(ErrorCode.PASSWORD_MISMATCH);

        if (userRepository.existsByEmail(request.getEmail()))
            throw new ApiException(ErrorCode.USER_EXISTS);

        if (!otpRepository.existsById(request.getOtp()))
            throw new ApiException(ErrorCode.OTP_NOT_FOUND);

        Users user = userMapper.toUser(request);
        String enCodePassword = passwordEncoder.encode(request.getPassword());
        user.setPasswordUser(enCodePassword);

        Roles role = roleRepository.findById(PredefinedRole.USER_ROLE)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));
        user.setRole(role);

        TypeUser typeUser = typeUserRepository.findById(PredefinedTypeUser.TYPE_USER_DEFAULT)
                .orElseThrow(() -> new ApiException(ErrorCode.TYPE_USER_NOT_FOUND));
        user.setTypeUser(typeUser);

        userRepository.save(user);
    }
}

