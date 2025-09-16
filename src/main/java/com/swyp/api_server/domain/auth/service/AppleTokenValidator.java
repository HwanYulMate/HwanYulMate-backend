package com.swyp.api_server.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swyp.api_server.common.http.CommonHttpClient;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Apple JWT 토큰 검증 서비스
 * - Apple 공개키를 이용한 JWT 서명 검증
 * - audience, issuer, 만료시간 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleTokenValidator {

    private final CommonHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${APPLE_CLIENT_ID:com.apptive.HwanYulMate}")
    private String appleClientId;
    
    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    
    // 공개키 캐시 (실제 운영에서는 Redis나 캐시 사용 권장)
    private Map<String, PublicKey> publicKeyCache = new HashMap<>();

    /**
     * Apple JWT 토큰 검증 및 Claims 반환
     * @param idToken Apple ID Token (JWT)
     * @return JWT Claims
     * @throws CustomException 토큰 검증 실패 시
     */
    public Claims validateAndParseClaims(String idToken) {
        try {
            log.debug("Apple ID Token 검증 시작");
            
            // 1. JWT 헤더에서 kid(Key ID) 추출
            String keyId = extractKeyId(idToken);
            
            // 2. Apple 공개키 조회
            PublicKey publicKey = getApplePublicKey(keyId);
            
            // 3. JWT 서명 검증 및 Claims 파싱
            Claims claims = Jwts.parser()
                    .setSigningKey(publicKey)
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(appleClientId)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();
            
            log.info("Apple JWT 토큰 검증 성공 - sub: {}", claims.getSubject());
            return claims;
            
        } catch (Exception e) {
            log.error("Apple JWT 토큰 검증 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID, 
                "Apple ID Token 검증 실패: " + e.getMessage());
        }
    }

    /**
     * JWT 헤더에서 Key ID (kid) 추출
     */
    private String extractKeyId(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            
            String header = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode headerNode = objectMapper.readTree(header);
            
            if (!headerNode.has("kid")) {
                throw new IllegalArgumentException("JWT header missing kid field");
            }
            
            return headerNode.get("kid").asText();
            
        } catch (Exception e) {
            log.error("JWT 헤더에서 kid 추출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID, 
                "JWT 헤더 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * Apple 공개키 조회 (캐시 사용)
     */
    private PublicKey getApplePublicKey(String keyId) {
        // 캐시에서 먼저 확인
        if (publicKeyCache.containsKey(keyId)) {
            log.debug("공개키 캐시에서 조회: {}", keyId);
            return publicKeyCache.get(keyId);
        }
        
        // Apple에서 공개키 목록 조회
        try {
            log.debug("Apple 공개키 API 호출: {}", APPLE_PUBLIC_KEYS_URL);
            JsonNode keysResponse = httpClient.getJson(APPLE_PUBLIC_KEYS_URL, Map.of());
            
            JsonNode keys = keysResponse.get("keys");
            if (keys == null || !keys.isArray()) {
                throw new IllegalStateException("Invalid Apple public keys response");
            }
            
            // keyId와 일치하는 키 찾기
            for (JsonNode keyNode : keys) {
                if (keyId.equals(keyNode.get("kid").asText())) {
                    PublicKey publicKey = buildPublicKey(keyNode);
                    publicKeyCache.put(keyId, publicKey);
                    log.info("Apple 공개키 조회 성공: {}", keyId);
                    return publicKey;
                }
            }
            
            throw new IllegalArgumentException("Public key not found for kid: " + keyId);
            
        } catch (Exception e) {
            log.error("Apple 공개키 조회 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID, 
                "Apple 공개키 조회 실패: " + e.getMessage());
        }
    }

    /**
     * JWK 정보로부터 RSA PublicKey 생성
     */
    private PublicKey buildPublicKey(JsonNode keyNode) {
        try {
            String kty = keyNode.get("kty").asText();
            if (!"RSA".equals(kty)) {
                throw new IllegalArgumentException("Unsupported key type: " + kty);
            }
            
            String nStr = keyNode.get("n").asText();
            String eStr = keyNode.get("e").asText();
            
            byte[] nBytes = Base64.getUrlDecoder().decode(nStr);
            byte[] eBytes = Base64.getUrlDecoder().decode(eStr);
            
            BigInteger modulus = new BigInteger(1, nBytes);
            BigInteger exponent = new BigInteger(1, eBytes);
            
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            return keyFactory.generatePublic(publicKeySpec);
            
        } catch (Exception e) {
            log.error("RSA 공개키 생성 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.OAUTH_TOKEN_INVALID, 
                "RSA 공개키 생성 실패: " + e.getMessage());
        }
    }
}