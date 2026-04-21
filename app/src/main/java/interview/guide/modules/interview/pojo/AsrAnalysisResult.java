package interview.guide.modules.interview.pojo;

import lombok.Data;

/**
 * ASR分析结果实体（JDK21兼容）
 */
@Data
public class AsrAnalysisResult {
    private String transcription;    // 语音转写文本
    private Integer confidenceScore; // 自信度（0-10）
    private Integer clarityScore;    // 清晰度（0-10）
    private String speechSpeed;      // 语速（如：中 | 200字/分钟）
    private String analysisReason;   // 分析依据
}