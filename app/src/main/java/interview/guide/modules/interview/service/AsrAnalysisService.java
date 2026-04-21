package interview.guide.modules.interview.service;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import interview.guide.modules.interview.pojo.AsrAnalysisResult;
import interview.guide.modules.interview.pojo.DashScopeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

// ... existing code ...
@Slf4j
@Service
@RequiredArgsConstructor
public class AsrAnalysisService {

    private final DashScopeProperties dashScopeProperties;
    private final ObjectMapper objectMapper;

    /**
     * 分析语音文件（通过 OSS URL）- 完整分析版本
     */
    public AsrAnalysisResult analyzeAudioByUrl(String audioUrl) throws Exception {
        log.info("开始 ASR 语音分析，URL: {}", audioUrl);

        // 构建详细的分析提示词
        String systemPrompt = """
                你是专业的语音情感与特征分析专家。请仔细聆听提供的语音文件，基于语音的实际特征进行分析。
                
                重要要求：
                1. 必须根据实际听到的语音内容进行转写和分析
                2. 禁止使用示例数据或模板数据
                3. 所有评分必须基于真实语音特征判断
                
                请返回标准 JSON 格式，字段说明：
                - transcription: 将语音内容逐字转写为文本
                - confidenceScore: 0-10 分，评估说话人的自信程度（流畅度、停顿、音量稳定性）
                - clarityScore: 0-10 分，评估紧张程度（颤抖、语速突变、清嗓子等）
                - speechSpeed: 语速评估，格式："慢/中/快 | 约 XX 字/分钟"
                - analysisReason: 50 字以内，说明评分依据和观察到的语音特征
                
                直接返回 JSON 对象，不要任何额外文字。
                """;

        // 1. 构建消息体
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Collections.singletonList(
                        Collections.singletonMap("audio", audioUrl)))
                .build();

        MultiModalMessage sysMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(
                        Collections.singletonMap("text", systemPrompt)))
                .build();

        // 2. 配置 ASR 参数
        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("enable_lid", dashScopeProperties.getAsr().getEnableLid());
        asrOptions.put("enable_itn", dashScopeProperties.getAsr().getEnableItn());

        // 3. 构建请求参数
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(dashScopeProperties.getApiKey())
                .model(dashScopeProperties.getAsr().getModel())
                .message(userMessage)
                .message(sysMessage)
                .parameter("asr_options", asrOptions)
                .build();

        // 4. 调用百炼 API 并解析结果
        try {
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationResult result = conv.call(param);

            log.info("百炼 ASR 原始返回：{}", JsonUtils.toJson(result));

            // 5. 获取结果
            if (result == null || result.getOutput() == null ||
                    result.getOutput().getChoices() == null ||
                    result.getOutput().getChoices().isEmpty()) {
                throw new RuntimeException("ASR 返回结果为空");
            }

            MultiModalMessage resultMessage = result.getOutput().getChoices().get(0).getMessage();
            List<Map<String, Object>> contentList = resultMessage.getContent();

            if (contentList == null || contentList.isEmpty()) {
                throw new RuntimeException("ASR 分析结果内容为空");
            }

            // 6. 提取 text 内容
            String jsonResult = null;
            for (Map<String, Object> contentItem : contentList) {
                if (contentItem.containsKey("text")) {
                    jsonResult = contentItem.get("text").toString().trim();
                    break;
                }
            }

            if (jsonResult == null || jsonResult.isEmpty()) {
                throw new RuntimeException("未从 ASR 结果中提取到文本内容");
            }

            log.info("提取到的 JSON 结果：{}", jsonResult);

            // 7. 验证 JSON 格式
            if (!jsonResult.startsWith("{") && !jsonResult.startsWith("[")) {
                log.error("ASR 返回的不是 JSON 格式：{}", jsonResult);
                throw new RuntimeException("ASR 返回格式错误，期望 JSON 格式");
            }

            // 8. 解析 JSON 为实体对象
            AsrAnalysisResult analysisResult = objectMapper.readValue(jsonResult, AsrAnalysisResult.class);

            // 9. 验证必填字段
            if (analysisResult.getTranscription() == null || analysisResult.getTranscription().isEmpty()) {
                analysisResult.setTranscription("");

            }
            // 如果转录内容为空，设置提示并返回默认值
            if (analysisResult.getTranscription().isEmpty()) {
                log.warn("ASR 未识别到有效语音内容，将使用默认值");
                analysisResult.setTranscription("[未识别到有效语音]");
                analysisResult.setConfidenceScore(0);
                analysisResult.setClarityScore(0);
                analysisResult.setSpeechSpeed("慢/中/快 | 约 0 字/分钟");
                analysisResult.setAnalysisReason("语音文件为空或无有效语音内容，无法评估自信度、紧张度或语速。");
                return analysisResult;
            }
            // 设置默认值（如果某些字段为空）
            if (analysisResult.getConfidenceScore() == null) {
                analysisResult.setConfidenceScore(5); // 默认中等分数
            }
            if (analysisResult.getClarityScore() == null) {
                analysisResult.setClarityScore(5);
            }
            if (analysisResult.getSpeechSpeed() == null) {
                analysisResult.setSpeechSpeed("中等");
            }
            if (analysisResult.getAnalysisReason() == null) {
                analysisResult.setAnalysisReason("基于语音特征的综合分析");
            }

            log.info("ASR 分析完成：转写长度={}, 自信度={}, 清晰度={}",
                    analysisResult.getTranscription().length(),
                    analysisResult.getConfidenceScore(),
                    analysisResult.getClarityScore());

            return analysisResult;

        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            log.error("百炼 ASR 调用失败", e);
            throw new RuntimeException("语音分析失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("ASR 结果解析失败", e);
            throw new RuntimeException("结果解析失败：" + e.getMessage());
        }
    }



    /**
     * 实时 ASR 识别 - 通过 MultipartFile 上传音频
     * 使用 qwen3-omni-flash-2025-12-01 模型的音频理解能力
     */
    public CompletableFuture<AsrAnalysisResult> recognizeRealTime(MultipartFile audioFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始实时 ASR 识别，文件名：{}, 大小：{} bytes",
                        audioFile.getOriginalFilename(), audioFile.getSize());

                byte[] audioData = audioFile.getBytes();

                String systemPrompt = """
                    你是专业的面试语音分析助手。请根据音频内容输出标准 JSON，字段如下：
                    - transcription: 完整转写文本
                    - confidenceScore: 0-10，自信度（流畅度、停顿控制、语气稳定性）
                    - clarityScore: 0-10，表达清晰度（发音清楚程度、句子连贯性）
                    - speechSpeed: 语速结论，格式为 "慢/中/快 | 约XX字/分钟"
                    - analysisReason: 30~80字，说明评分依据

                    约束：
                    1. 必须基于音频真实内容，不可编造。
                    2. 若部分内容无法辨识，transcription 中用 [听不清] 标注。
                    3. 仅返回 JSON，不要返回多余文本或 markdown。
                    """;

                MultiModalMessage userMessage = MultiModalMessage.builder()
                        .role(Role.USER.getValue())
                        .content(Collections.singletonList(
                                Collections.singletonMap("audio", audioData)))
                        .build();

                MultiModalMessage sysMessage = MultiModalMessage.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(Collections.singletonList(
                                Collections.singletonMap("text", systemPrompt)))
                        .build();

                Map<String, Object> asrOptions = new HashMap<>();
                asrOptions.put("enable_lid", true);
                asrOptions.put("enable_itn", false);

                MultiModalConversationParam param = MultiModalConversationParam.builder()
                        .apiKey(dashScopeProperties.getApiKey())
                        .model(dashScopeProperties.getAsr().getModel())
                        .message(userMessage)
                        .message(sysMessage)
                        .parameter("asr_options", asrOptions)
                        .build();

                MultiModalConversation conv = new MultiModalConversation();
                MultiModalConversationResult result = conv.call(param);

                if (result == null || result.getOutput() == null ||
                        result.getOutput().getChoices() == null ||
                        result.getOutput().getChoices().isEmpty()) {
                    throw new RuntimeException("ASR 返回结果为空");
                }

                MultiModalMessage resultMessage = result.getOutput().getChoices().get(0).getMessage();
                List<Map<String, Object>> contentList = resultMessage.getContent();

                if (contentList == null || contentList.isEmpty()) {
                    throw new RuntimeException("ASR 分析结果内容为空");
                }

                String resultText = null;
                for (Map<String, Object> contentItem : contentList) {
                    if (contentItem.containsKey("text")) {
                        resultText = contentItem.get("text").toString().trim();
                        break;
                    }
                }

                if (resultText == null || resultText.isEmpty()) {
                    throw new RuntimeException("未识别到任何内容");
                }

                AsrAnalysisResult analysisResult = parseOrFallback(resultText);

                log.info("实时 ASR 识别完成：转写长度={}", analysisResult.getTranscription() == null ? 0 : analysisResult.getTranscription().length());

                return analysisResult;

            } catch (IOException e) {
                log.error("读取音频文件失败", e);
                throw new RuntimeException("读取音频文件失败：" + e.getMessage(), e);
            } catch (ApiException | NoApiKeyException | UploadFileException e) {
                log.error("百炼 ASR 调用失败", e);
                throw new RuntimeException("语音识别失败：" + e.getMessage(), e);
            } catch (Exception e) {
                log.error("ASR 识别失败", e);
                throw new RuntimeException("识别失败：" + e.getMessage(), e);
            }
        });
    }

    private AsrAnalysisResult parseOrFallback(String resultText) {
        try {
            String normalized = resultText.trim();
            if (normalized.startsWith("{") || normalized.startsWith("[")) {
                AsrAnalysisResult parsed = objectMapper.readValue(normalized, AsrAnalysisResult.class);
                fillDefaults(parsed);
                return parsed;
            }
        } catch (Exception e) {
            log.warn("实时 ASR JSON 解析失败，回退纯文本转写: {}", e.getMessage());
        }

        AsrAnalysisResult fallback = new AsrAnalysisResult();
        fallback.setTranscription(resultText);
        fallback.setConfidenceScore(5);
        fallback.setClarityScore(5);
        fallback.setSpeechSpeed("中 | 约未知字/分钟");
        fallback.setAnalysisReason("模型返回了纯文本转写，已使用默认评分回退");
        return fallback;
    }

    private void fillDefaults(AsrAnalysisResult analysisResult) {
        if (analysisResult.getTranscription() == null) {
            analysisResult.setTranscription("");
        }
        if (analysisResult.getConfidenceScore() == null) {
            analysisResult.setConfidenceScore(5);
        }
        if (analysisResult.getClarityScore() == null) {
            analysisResult.setClarityScore(5);
        }
        if (analysisResult.getSpeechSpeed() == null || analysisResult.getSpeechSpeed().isBlank()) {
            analysisResult.setSpeechSpeed("中 | 约未知字/分钟");
        }
        if (analysisResult.getAnalysisReason() == null || analysisResult.getAnalysisReason().isBlank()) {
            analysisResult.setAnalysisReason("基于语音内容完成自动分析");
        }
    }
}

