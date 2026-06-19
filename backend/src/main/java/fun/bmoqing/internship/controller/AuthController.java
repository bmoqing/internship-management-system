package fun.bmoqing.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import fun.bmoqing.internship.common.PasswordPolicyUtil;
import fun.bmoqing.internship.common.Result; // 确保Result也在common包下
import fun.bmoqing.internship.entity.User;
import fun.bmoqing.internship.mapper.UserMapper;
import fun.bmoqing.internship.security.LoginAttemptService;
import fun.bmoqing.internship.security.JwtUtil;
import fun.bmoqing.internship.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class LoginResponse {
        private String token;
        private String refreshToken;
        private Long accessExpireMs;
        private Long refreshExpireMs;
        private User user;
    }

    @Data
    static class RefreshRequest {
        private String refreshToken;
    }

    @Data
    static class LogoutRequest {
        private String refreshToken;
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            return Result.validationError("用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            return Result.validationError("密码不能为空");
        }
        if (!StringUtils.hasText(user.getName())) {
            return Result.validationError("姓名不能为空");
        }
        if (user.getUsername().length() < 4 || user.getUsername().length() > 20) {
            return Result.validationError("用户名长度需在4-20位之间");
        }
        if (user.getTeacherId() == null) {
            return Result.validationError("请选择负责教师");
        }

        String passwordPolicyError = PasswordPolicyUtil.validate(user.getPassword());
        if (passwordPolicyError != null) {
            return Result.validationError(passwordPolicyError);
        }

        // 1. 校验用户名是否存在
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        if (userMapper.selectCount(query) > 0) {
            return Result.conflict("用户名已存在");
        }

        User teacher = userMapper.selectById(user.getTeacherId());
        if (teacher == null || !"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            return Result.validationError("负责教师无效，请重新选择");
        }
        // 2. 默认设置
        user.setRole("STUDENT"); // 注册的默认是学生
        user.setCompanyId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return Result.success(null);
    }

    @GetMapping("/teacher-options")
    public Result<?> teacherOptions() {
        return Result.success(userMapper.selectByRole("TEACHER"));
    }
    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return Result.validationError("账号和密码不能为空");
        }

        String loginKey = request.getUsername() + "@" + getClientIp(httpRequest);
        if (loginAttemptService.isBlocked(loginKey)) {
            return Result.tooManyRequests("登录失败次数过多，请15分钟后再试");
        }

        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", request.getUsername());
        User user = userMapper.selectOne(query);

        if (user == null) {
            loginAttemptService.onLoginFail(loginKey);
            return Result.unauthorized("用户名或密码错误");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            loginAttemptService.onLoginFail(loginKey);
            return Result.unauthorized("用户名或密码错误");
        }

        boolean encodedMatched;
        try {
            encodedMatched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        } catch (Exception ex) {
            encodedMatched = false;
        }

        if (!encodedMatched) {
            loginAttemptService.onLoginFail(loginKey);
            int remainingAttempts = loginAttemptService.getRemainingAttempts(loginKey);
            if (remainingAttempts > 0) {
                return Result.unauthorized("用户名或密码错误，还可尝试" + remainingAttempts + "次");
            }
            return Result.unauthorized("用户名或密码错误");
        }

        loginAttemptService.onLoginSuccess(loginKey);

        return Result.success(buildLoginResponse(user));
    }
//刷新token
    @PostMapping("/refresh")
    public Result<?> refresh(@RequestBody RefreshRequest request) {
        if (request == null || !StringUtils.hasText(request.getRefreshToken())) {
            return Result.unauthorized("刷新令牌不能为空");
        }

        String refreshToken = request.getRefreshToken();
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            return Result.unauthorized("登录已失效，请重新登录");
        }

        Claims claims;
        try {
            claims = jwtUtil.parseClaims(refreshToken);
        } catch (Exception ex) {
            return Result.unauthorized("刷新令牌无效或已过期");
        }

        String tokenType = claims.get("tokenType", String.class);
        if (!"REFRESH".equalsIgnoreCase(tokenType)) {
            return Result.unauthorized("令牌类型错误");
        }

        Long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (Exception ex) {
            return Result.unauthorized("刷新令牌无效");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.unauthorized("用户不存在");
        }

        String tokenRole = claims.get("role", String.class);
        if (!StringUtils.hasText(tokenRole) || !tokenRole.equalsIgnoreCase(user.getRole())) {
            return Result.unauthorized("用户权限已变化，请重新登录");
        }

        tokenBlacklistService.blacklist(refreshToken, jwtUtil.getExpireAtMillis(refreshToken));
        return Result.success(buildLoginResponse(user));
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestBody(required = false) LogoutRequest request,
                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.substring(7);
            blacklistTokenQuietly(accessToken);
        }

        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            blacklistTokenQuietly(request.getRefreshToken());
        }

        return Result.success(null);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        user.setPassword(null);

        LoginResponse response = new LoginResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setAccessExpireMs(jwtUtil.getAccessExpireMs());
        response.setRefreshExpireMs(jwtUtil.getRefreshExpireMs());
        response.setUser(user);
        return response;
    }

    private void blacklistTokenQuietly(String token) {
        try {
            long expireAt = jwtUtil.getExpireAtMillis(token);
            tokenBlacklistService.blacklist(token, expireAt);
        } catch (Exception ignored) {
            // no-op
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
