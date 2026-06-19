package fun.bmoqing.internship.security;

import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserMapper userMapper, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");//自动地把Token注入到Authorization请求头当中

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!tokenBlacklistService.isBlacklisted(token)) {
                try {
                    Claims claims = jwtUtil.parseClaims(token);
                    String subject = claims.getSubject();
                    String role = claims.get("role", String.class);
                    String tokenType = claims.get("tokenType", String.class);

                    if (!StringUtils.hasText(tokenType) || "ACCESS".equalsIgnoreCase(tokenType)) {
                        if (StringUtils.hasText(subject)
                                && StringUtils.hasText(role)
                                && SecurityContextHolder.getContext().getAuthentication() == null) {
                            Long userId = Long.parseLong(subject);
                            User user = userMapper.selectById(userId);

                            if (user != null && role.equalsIgnoreCase(user.getRole())) {
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                                        );
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        }
                    }
                } catch (JwtException | IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                }
            } else {
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
