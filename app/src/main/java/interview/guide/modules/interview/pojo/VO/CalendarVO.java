package interview.guide.modules.interview.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarVO {
    private int day;
    private String assignment;
}
