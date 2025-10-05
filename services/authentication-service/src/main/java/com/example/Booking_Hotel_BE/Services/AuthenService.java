package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Dto.Request.AuthenRequest;
import com.example.Booking_Hotel_BE.Dto.Request.IntrospectRequest;
import com.example.Booking_Hotel_BE.Dto.Response.AuthenResponse;
import com.example.Booking_Hotel_BE.Dto.Response.IntrospectResponse;
import com.example.Booking_Hotel_BE.Exception.ApiException;
import com.example.Booking_Hotel_BE.Exception.ErrorCode;
import com.example.Booking_Hotel_BE.Mappers.UserMapper;
import com.example.Booking_Hotel_BE.Models.Images;
import com.example.Booking_Hotel_BE.Models.Invalidated_Token;
import com.example.Booking_Hotel_BE.Models.Users;
import com.example.Booking_Hotel_BE.Repositories.ImageRepository;
import com.example.Booking_Hotel_BE.Repositories.InvalidatedTokenRepository;
import com.example.Booking_Hotel_BE.Repositories.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenService
{
    UserRepository userRepository;
    RefreshTokenService refreshTokenService;
    InvalidatedTokenRepository invalidatedTokenRepository;
    ImageRepository imageRepository;
    UserMapper userMapper;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected Long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected Long REFRESHABLE_DURATION;


    // Hàm xác thực Token
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException
    {
        var token = request.getToken();
        UUID idUser = null;
        String role = null;

        boolean isValid = true;
        try {
            // verifyToken trả về SignedJWT
            SignedJWT signedJWT = verifyToken(token);

            // Lấy claims từ SignedJWT
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            String idUserStr = claims.getStringClaim("idUser");
            if (idUserStr != null) {
                idUser = UUID.fromString(idUserStr);
            }

            role = claims.getStringClaim("scope");
        }catch (ApiException e){
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .idUser(idUser)
                .role(role)
                .build();
    }

    // Hàm xác thực đăng nhập
    public AuthenResponse authenticated(AuthenRequest request, HttpServletResponse response)
    {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordUser()))
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordUser());

        if (!authenticated) throw new ApiException(ErrorCode.UNAUTHENTICATED);

        var token = generateAccessToken(user);
        var refreshToken = generateRefreshToken(user);

        refreshTokenService.saveRefreshToken(
                user.getIdUser().toString(),
                refreshToken,
                REFRESHABLE_DURATION
        );
        // Set refreshToken cookie
        String cookieHeader = String.format(
                "refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
                refreshToken,
                REFRESHABLE_DURATION
        );
        response.addHeader("Set-Cookie", cookieHeader);

        String avatarUrl = imageRepository.findByRefId(user.getIdUser().toString())
                .stream()
                .map(Images::getUrl)
                .findFirst()
                .orElse(null);

        var userResponse = userMapper.toUserResponse(user);
        userResponse.setAvatar(avatarUrl);

        return AuthenResponse.builder()
                .token(token)
                .infUser(userResponse)
                .build();
    }

    // Hàm Refresh Token
    public AuthenResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        log.info("=== [REFRESH TOKEN] Bắt đầu xử lý ===");

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.warn("[REFRESH TOKEN] Không tìm thấy cookie trong request");
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Không có cookie trong request");
        }

        String refreshToken = Arrays.stream(cookies)
                .filter(c -> "refreshToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);

        if (refreshToken == null) {
            log.warn("[REFRESH TOKEN] Không tìm thấy cookie tên refreshToken");
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Không tìm thấy refreshToken trong cookie");
        }

        log.info("[REFRESH TOKEN] Token nhận được từ cookie: {}", refreshToken.substring(0, 20) + "...");

        SignedJWT signToken;
        try {
            signToken = verifyToken(refreshToken);
            log.info("[REFRESH TOKEN] Token verify thành công");
        } catch (JOSEException | ParseException e) {
            log.error("[REFRESH TOKEN] Lỗi verify token: {}", e.getMessage());
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token không hợp lệ");
        }

        String idUserStr;
        try {
            idUserStr = signToken.getJWTClaimsSet().getStringClaim("idUser");
            log.info("[REFRESH TOKEN] idUser trong token: {}", idUserStr);
        } catch (Exception e) {
            log.error("[REFRESH TOKEN] Không đọc được claim idUser từ token: {}", e.getMessage());
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID, "Thiếu claim idUser trong refresh token");
        }

        UUID idUser;
        try {
            idUser = UUID.fromString(idUserStr);
        } catch (IllegalArgumentException e) {
            log.error("[REFRESH TOKEN] idUser không hợp lệ: {}", idUserStr);
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID, "idUser không hợp lệ trong refresh token");
        }

        var user = userRepository.findById(idUser).orElse(null);
        if (user == null) {
            log.warn("[REFRESH TOKEN] Không tìm thấy người dùng trong DB với idUser={}", idUserStr);
            throw new ApiException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng");
        }
        log.info("[REFRESH TOKEN] Người dùng tồn tại trong DB: {}", user.getEmail());

        String storedRefreshToken = refreshTokenService.getRefreshToken(idUserStr);
        if (storedRefreshToken == null) {
            log.warn("[REFRESH TOKEN] Không tìm thấy refresh token trong Redis với idUser={}", idUserStr);
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Refresh token không tồn tại trong Redis");
        }
        if (!storedRefreshToken.equals(refreshToken)) {
            log.warn("[REFRESH TOKEN] Refresh token không trùng khớp trong Redis cho user={}", idUserStr);
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Refresh token không khớp với Redis");
        }
        log.info("[REFRESH TOKEN] Refresh token hợp lệ trong Redis");

        String newAccessToken = generateAccessToken(user);
        String newRefreshToken = generateRefreshToken(user);
        log.info("[REFRESH TOKEN] Đã tạo token mới thành công");

        refreshTokenService.saveRefreshToken(idUserStr, newRefreshToken, REFRESHABLE_DURATION);
        log.info("[REFRESH TOKEN] Đã lưu refresh token mới vào Redis");

        String cookieHeader = String.format(
                "refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
                newRefreshToken, REFRESHABLE_DURATION
        );
        response.addHeader("Set-Cookie", cookieHeader);
        log.info("[REFRESH TOKEN] Đã gửi cookie refreshToken mới về FE");

        String avatarUrl = null;
        try {
            avatarUrl = imageRepository.findByRefId(idUserStr)
                    .stream()
                    .map(Images::getUrl)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[REFRESH TOKEN] Không thể lấy avatar: {}", e.getMessage());
        }

        var userResponse = userMapper.toUserResponse(user);
        userResponse.setAvatar(avatarUrl);

        log.info("[REFRESH TOKEN] Hoàn tất refresh token cho user {}", idUserStr);
        return AuthenResponse.builder()
                .token(newAccessToken)
                .infUser(userResponse)
                .build();
    }

    // Hàm Logout Token
    public void Logout (HttpServletRequest request, HttpServletResponse response) throws ParseException, JOSEException {
        try {
            // Lấy refreshToken từ cookie
            String refreshToken = Arrays.stream(request.getCookies())
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

            var signToken = verifyToken(refreshToken);

            var idUser = signToken.getJWTClaimsSet().getStringClaim("idUser"); // Lấy idUser của Token
            refreshTokenService.deleteRefreshToken(idUser); // Xóa refresh Token trong redis

            String jit = signToken.getJWTClaimsSet().getJWTID();

            Date expirationTime = signToken.getJWTClaimsSet().getExpirationTime();

            Invalidated_Token invalidatedToken = Invalidated_Token.builder()
                    .idToken(UUID.fromString(jit))
                    .expiryTime(expirationTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);

            // Xóa cookie chứa refreshToken
            response.addHeader("Set-Cookie", "refreshToken=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=None");
        }catch (ApiException e){
            log.error("Cannot verify token", e);
        }
    }

    // Hàm tạo Access Token
    public String generateAccessToken(Users user)
    {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder() // Nội dung của Payload
                .subject(user.getEmail())
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("idUser", user.getIdUser())
                .build();


        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create TOKE", e);
            throw new RuntimeException(e);
        }
    }

    // Hàm tạo Refresh Token
    public String generateRefreshToken(Users user)
    {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder() // Nội dung của Payload
                .subject(user.getEmail())
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("idUser", user.getIdUser())
                .build();


        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create TOKE", e);
            throw new RuntimeException(e);
        }
    }

    // Hàm build Scope
    private String buildScope(Users user)
    {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (user.getRole() != null) {
            stringJoiner.add("ROLE_" + user.getRole().getRoleName());
        }
        return stringJoiner.toString();
    }

    // Hàm Verify Token
    private SignedJWT verifyToken(String token) throws JOSEException, ParseException
    {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime(); // Lấy thời hạn của token

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new ApiException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(UUID.fromString(signedJWT.getJWTClaimsSet().getJWTID())))
            throw new ApiException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

}
