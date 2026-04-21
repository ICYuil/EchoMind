package interview.guide.modules.interview.repository;

import interview.guide.modules.interview.pojo.Calendar;
import interview.guide.modules.interview.pojo.CardRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalendarReposity extends JpaRepository<Calendar,Long> {
    Calendar findByUserIdAndYearAndMonthAndDayOrderByIdDesc(Long userId, int year, int month,int day);
    List<Calendar> findByUserIdAndYearAndMonthOrderByIdDesc(Long userId, int year, int month);

}
