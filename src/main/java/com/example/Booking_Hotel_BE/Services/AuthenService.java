package com.example.Booking_Hotel_BE.Services;

import com.example.Booking_Hotel_BE.Dto.Request.AuthenRequest;
import com.example.Booking_Hotel_BE.Dto.Request.IntrospectRequest;
import com.example.Booking_Hotel_BE.Dto.Request.LogoutRequest;
import com.example.Booking_Hotel_BE.Dto.Response.AuthenResponse;
import com.example.Booking_Hotel_BE.Dto.Response.IntrospectResponse;
import com.example.Booking_Hotel_BE.Exception.ApiException;
import com.example.Booking_Hotel_BE.Exception.ErrorCode;
import com.example.Booking_Hotel_BE.Models.Invalidated_Token;
import com.example.Booking_Hotel_BE.Models.Users;
import com.example.Booking_Hotel_BE.Repositories.InvalidatedTokenRepository;
import com.example.Booking_Hotel_BE.Repositories.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
    InvalidatedTokenRepository invalidatedTokenRepository;

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
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();

        boolean isValid = true;
        try {
            verifyToken(token, false);
        }catch (ApiException e){
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    // Hàm xác thực đăng nhập
    public AuthenResponse authenticated(AuthenRequest request)
    {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordUser());

        if (!authenticated) throw new ApiException(ErrorCode.UNAUTHENTICATED);

        var token = generateAccessToken(user);
        var refreshToken = generateRefreshToken(user);

        return AuthenResponse.builder()
                .idUser(user.getIdUser().toString())
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    // Hàm Refresh Token

    // Hàm Logout Token
    public void Logout (LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);
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
    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException
    {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .plus(REFRESHABLE_DURATION, ChronoUnit.MINUTES)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new ApiException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(UUID.fromString(signedJWT.getJWTClaimsSet().getJWTID())))
            throw new ApiException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

}
