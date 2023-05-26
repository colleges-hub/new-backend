package ru.ncti.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.ncti.backend.entity.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Ivan Chuvilin (@ichuvilin)
 * Date: 26-05-2023
 */
@Component
public class JwtTokenUtil {
    
    @Value("${jwtSecret}")
    private String secret;
    private final Long jwtExpirationInMs = 604800000L; // 7 days
    private final Long refreshExpirationInMs = 1209600000L; // 14 days

    public String generateToken(User userDetails) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("user_id", userDetails.getId());
        claims.put("role", userDetails.getAuthorities());

        String subject = userDetails.getUsername();
        return Jwts.builder().setClaims(claims)
                .setSubject(subject)
                .setIssuer("ncti-backend")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    public String generateRefreshToken(User userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String subject = userDetails.getUsername();
        return Jwts.builder().setClaims(claims)
                .setSubject(subject)
                .setIssuer("ncti-backend")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    public Boolean validateToken(String token, User userDetails) {
        final String username = getUsernameFromToken(token);
        String usernameOrEmail = userDetails.getUsername();
        return (username.equals(usernameOrEmail) && !isTokenExpired(token));
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    private Date getExpirationDateFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getExpiration();
    }

    public boolean validateRefreshToken(String refreshToken, User userDetails) {
        final String username = getUsernameFromToken(refreshToken);
        String usernameOrEmail = userDetails.getUsername();
        return (username.equals(usernameOrEmail) && !isRefreshTokenExpired(refreshToken));
    }

    private boolean isRefreshTokenExpired(String refreshToken) {
        final Date expiration = getExpirationDateFromToken(refreshToken);
        return expiration.before(new Date());
    }
}
