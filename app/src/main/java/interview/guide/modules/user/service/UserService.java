package interview.guide.modules.user.service;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.security.JwtTokenProvider;
import interview.guide.modules.user.model.LoginRequest;
import interview.guide.modules.user.model.LoginResponse;
import interview.guide.modules.user.model.RegisterRequest;
import interview.guide.modules.user.model.UserEntity;
import interview.guide.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱已被注册");
        }

        // 创建新用户
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() != null && !request.nickname().isBlank() 
            ? request.nickname() 
            : request.username());
        user.setPhone(request.phone());
        user.setEnabled(true);

        UserEntity saved = userRepository.save(user);
        log.info("用户注册成功: userId={}, username={}, email={}", 
            saved.getId(), saved.getUsername(), saved.getEmail());
    }

    /**
     * 用户登录
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        UserEntity user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误"));

        // 验证密码

        // 打印前端传的密码
        System.out.println("用户输入密码：[" + request.password() + "]");
// 打印数据库取出来的哈希
        System.out.println("数据库哈希：[" + user.getPassword() + "]");
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误");
        }

        // 检查用户是否被禁用
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账户已被禁用");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成JWT Token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getEmail());

        log.info("用户登录成功: userId={}, username={}, email={}", 
            user.getId(), user.getUsername(), user.getEmail());

        return new LoginResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getNickname(),
            user.getAvatar(),
            token,
            user.getCreatedAt()
        );
    }

    /**
     * 根据ID获取用户
     */
    public UserEntity getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    /**
     * 根据邮箱获取用户
     */
    public UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }
}