package interview.guide.modules.knowledgebase.repository;

import interview.guide.modules.knowledgebase.model.TestQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestionEntity,Long> {
    /**
     * 根据知识库名称查询是否存在数据
     * @param databaseId 知识库名称
     * @return true 表示存在，false 表示不存在
     */

    boolean existsByDatabaseId(Long databaseId);
    TestQuestionEntity getTestQuestionEntitiesById(Long id);
}
