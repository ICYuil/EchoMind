package interview.guide.modules.user.model;

import java.time.LocalDateTime;

public record LoginResponse(
    Long userId,
    String username,
    String email,
    String nickname,
    String avatar,
    String token,
    LocalDateTime createdAt
) {
}