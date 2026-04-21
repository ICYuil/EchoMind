package interview.guide.modules.interview.repository;

import interview.guide.modules.interview.model.InterviewAnswerEntity;
import interview.guide.modules.interview.model.InterviewSessionEntity;
import interview.guide.modules.interview.pojo.CardRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRecordRepository extends JpaRepository<CardRecord,Long> {
    CardRecord findByUserIdAndYearAndMonthOrderByIdDesc(Long userId, int year, int month);

}
