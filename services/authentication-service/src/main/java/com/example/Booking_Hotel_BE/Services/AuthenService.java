package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Dto.Request.AuthenRequest;
import com.example.Booking_Hotel_BE.Dto.Request.IntrospectRequest;
import com.example.Booking_Hotel_BE.Dto.Request.LogoutRequest;
import com.example.Booking_Hotel_BE.Dto.Response.AuthenResponse;
import com.example.Booking_Hotel_BE.Dto.Response.IntrospectResponse;
import com.example.Booking_Hotel_BE.Exception.ApiException;
import com.example.Booking_Hotel_BE.Exception.ErrorCode;
import com.example.Booking_Hotel_BE.Mappers.UserMapper;
import com.example.Booking_Hotel_BE.Models.Invalidated_Token;
import com.example.Booking_Hotel_BE.Models.Users;
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

        boolean isValid = true;
        try {
            verifyToken(token);
        }catch (ApiException e){
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
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
        // Tạo Cookie lưu refreshToken
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(Math.toIntExact(REFRESHABLE_DURATION));

        // Gắn Cookie vào response
        response.addCookie(refreshTokenCookie);

        return AuthenResponse.builder()
                .token(token)
                .infUser(userMapper.toUserResponse(user))
                .build();
    }

    // Hàm Refresh Token
    public AuthenResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws ParseException, JOSEException
    {
        // Lấy refreshToken từ cookie
        String refreshToken = Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

        var signToken = verifyToken(refreshToken);

        var idUserStr = signToken.getJWTClaimsSet().getStringClaim("idUser");
        UUID idUser = UUID.fromString(idUserStr);


        var user = userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // Lấy refresh token trong Redis
        String storedRefreshToken = refreshTokenService.getRefreshToken(idUserStr);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }

        var newAccessToken = generateAccessToken(user);
        var newRefreshToken = generateRefreshToken(user);

        refreshTokenService.saveRefreshToken(
                user.getIdUser().toString(),
                newRefreshToken,
                REFRESHABLE_DURATION
        );

        // Tạo Cookie lưu refreshToken
        Cookie newRefreshTokenCookie = new Cookie("refreshToken", newRefreshToken);
        newRefreshTokenCookie.setHttpOnly(true);
        newRefreshTokenCookie.setSecure(false);
        newRefreshTokenCookie.setPath("/");
        newRefreshTokenCookie.setMaxAge(Math.toIntExact(REFRESHABLE_DURATION));
        response.addCookie(newRefreshTokenCookie);

        return AuthenResponse.builder()
                .token(newAccessToken)
                .build();
    }

    // Hàm Logout Token
    public void Logout (LogoutRequest request) throws ParseException, JOSEException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(request.getToken());

            var idUser = signedJWT.getJWTClaimsSet().getStringClaim("idUser"); // Lấy idUser của Token
            var signToken = verifyToken(request.getToken());
            refreshTokenService.deleteRefreshToken(idUser); // Xóa refresh Token trong redis

            String jit = signToken.getJWTClaimsSet().getJWTID();

            Date expirationTime = signToken.getJWTClaimsSet().getExpirationTime();

            Invalidated_Token invalidatedToken = Invalidated_Token.builder()
                    .idToken(UUID.fromString(jit))
                    .expiryTime(expirationTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
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
