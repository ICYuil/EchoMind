package interview.guide.modules.interview.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddQuestionDTO {
    public int questionIndex;
    public String addQuestion;
    public int addQuestionIndex;
}
