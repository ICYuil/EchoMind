package interview.guide.common.security;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.common.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public final class SecurityUtils {

    private final JwtTokenProvider jwtTokenProvider;

    public  UserPrincipal currentUserOrThrow() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        
        String token = extractTokenFromRequest(attrs.getRequest());
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少认证信息，请先登录");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "认证信息已失效，请重新登录");
        }

        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            return new UserPrincipal(userId, username, email);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法认证信息");
        }
    }

    public Long getCurrentUserIdOrThrow() {
        return currentUserOrThrow().userId();
    }

    public String getCurrentUsernameOrThrow() {
        return currentUserOrThrow().username();
    }

    private static String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        String xToken = request.getHeader("X-Auth-Token");
        if (xToken != null && !xToken.isBlank()) {
            return xToken;
        }

        return null;
    }

    public record UserPrincipal(Long userId, String username, String email) {}
}
