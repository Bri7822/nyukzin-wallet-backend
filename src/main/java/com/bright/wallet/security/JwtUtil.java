package com.bright.wallet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24 hours

    /**
     * Generate a token containing the user's email AND role.
     * Both are stored inside the token so JwtFilter can read them
     * on every request without hitting the database again.
     *
     * "claim" = a piece of data stored inside the token payload.
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)                           // who the token belongs to
                .claim("role", role)                         // store role as a custom claim
                .setIssuedAt(new Date())                     // created now
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(secretKey)
                .compact();
    }

    // Extract email from token (stored in "subject")
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // Extract role from token (stored as a custom claim)
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
        // .get("role") reads the value we stored with .claim("role", role)
    }

    // Check token is not expired and signature is valid
    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}