package interview.guide.modules.user;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.common.security.SecurityUtils;
import interview.guide.modules.user.model.LoginRequest;
import interview.guide.modules.user.model.LoginResponse;
import interview.guide.modules.user.model.RegisterRequest;
import interview.guide.modules.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;
    /**
     * 用户注册
     */
    @PostMapping("/register")
    @RateLimit(dimensions = {RateLimit.Dimension.IP}, count = 3)
    public Result register(@Valid @RequestBody RegisterRequest request) {
        log.info("收到用户注册请求: username={}, email={}", request.username(), request.email());
        userService.register(request);
        return Result.success("注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @RateLimit(dimensions = {RateLimit.Dimension.IP}, count = 5)
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("收到用户登录请求: email={}", request.email());
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /**
     * 用户信息
     */
    @GetMapping("/info")
    public Result<interview.guide.modules.user.model.UserEntity> getUserInfo() {
        Long userId = securityUtils.currentUserOrThrow().userId();
        interview.guide.modules.user.model.UserEntity user = userService.getUserById(userId);
        return Result.success(user);
    }

   /* *//**
     * 健康检查接口
     *//*
    @GetMapping("/health")
    public Result<java.util.Map<String, String>> health() {
        return Result.success(java.util.Map.of(
            "status", "UP",
            "service", "AI Interview Platform - User Service"
        ));
    }*/
}