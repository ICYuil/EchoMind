package interview.guide.modules.interview.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ScoreEntity {
    private List<ScoreQuestionEntity> scoreQuestionEntity;
    private Integer overallScore;
}

