package interview.guide.modules.interview.repository;

import interview.guide.modules.interview.pojo.SessionToBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionToBaseIdReposity extends JpaRepository<SessionToBaseEntity,Integer> {
    List<SessionToBaseEntity>findSessionToBaseEntitiesBySessionId(String sessionId);
}
