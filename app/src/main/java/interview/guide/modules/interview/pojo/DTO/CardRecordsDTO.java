package interview.guide.modules.interview.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardRecordsDTO {
private  Long userId;
private int year;
private int month;
private  int day;
}
