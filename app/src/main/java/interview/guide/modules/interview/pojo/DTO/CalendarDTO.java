package interview.guide.modules.interview.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalendarDTO {
private Long userId;
private int year;
private int month;
private int day;
private String assignment;
}
