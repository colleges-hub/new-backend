package ru.collegehub.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.collegehub.backend.model.User;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private final String secret;
    @Value("${jwt.expiration}")
    private final Long jwtExpirationInMs;
    @Value("${jwt.refresh_expiration}")
    private final Long refreshExpirationInMs;

    public String generateToken(User userDetails, Collection<? extends GrantedAuthority> authorities) {
        return getString(userDetails, authorities, jwtExpirationInMs);
    }

    public String generateRefreshToken(User userDetails, Collection<? extends GrantedAuthority> authorities) {
        return getString(userDetails, authorities, refreshExpirationInMs);
    }

    private String getString(User userDetails, Collection<? extends GrantedAuthority> authorities, Long expirationInMs) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("user_id", userDetails.getId());
        claims.put("roles", authorities);

        String subject = userDetails.getEmail();
        return Jwts.builder().setClaims(claims)
                .setSubject(subject)
                .setIssuer("backend")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationInMs))
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String email = getEmailFromToken(token);
        String username = userDetails.getUsername();
        return (username.equals(email) && !isTokenExpired(token));
    }

    public String getEmailFromToken(String token) {
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
        final String email = getEmailFromToken(refreshToken);
        String usernameOrEmail = userDetails.getEmail();
        return (email.equals(usernameOrEmail) && !isRefreshTokenExpired(refreshToken));
    }

    private boolean isRefreshTokenExpired(String refreshToken) {
        final Date expiration = getExpirationDateFromToken(refreshToken);
        return expiration.before(new Date());
    }
}