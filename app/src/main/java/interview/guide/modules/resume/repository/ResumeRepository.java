package interview.guide.modules.resume.repository;

import interview.guide.modules.resume.model.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 简历Repository
 */
@Repository
public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {
    
    /**
     * 根据文件哈希查找简历（用于去重）
     */
    Optional<ResumeEntity> findByFileHashAndUserId(String fileHash, Long userId);
    
    /**
     * 检查文件哈希是否存在
     */
    boolean existsByFileHashAndUserId(String fileHash, Long userId);

    java.util.List<ResumeEntity> findByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<ResumeEntity> findByIdAndUserId(Long id, Long userId);
}
