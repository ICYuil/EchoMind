package interview.guide.modules.knowledgebase.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "test_question_base")
public class TestQuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //知识库名称
    private  Long databaseId;
    //题目
    private String content;
    //答案
    private String ans;

}
