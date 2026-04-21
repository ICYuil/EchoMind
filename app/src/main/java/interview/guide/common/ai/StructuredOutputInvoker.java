package interview.guide.common.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一封装结构化输出调用与重试策略。
 */
@Component
public class StructuredOutputInvoker {

    private static final String STRICT_JSON_INSTRUCTION = """
请仅返回可被 JSON 解析器直接解析的 JSON 对象，并严格满足字段结构要求：
1) 不要输出 Markdown 代码块（如 ```json）。
2) 不要输出任何解释文字、前后缀、注释。
3) 所有字符串内引号必须正确转义。
""";

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.MULTILINE);
    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",\\s*([\\]}])");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int maxAttempts;
    private final boolean includeLastErrorInRetryPrompt;

    public StructuredOutputInvoker(
        @Value("${app.ai.structured-max-attempts:2}") int maxAttempts,
        @Value("${app.ai.structured-include-last-error:true}") boolean includeLastErrorInRetryPrompt
    ) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.includeLastErrorInRetryPrompt = includeLastErrorInRetryPrompt;
    }

    public <T> T invoke(
        ChatClient chatClient,
        String systemPromptWithFormat,
        String userPrompt,
        BeanOutputConverter<T> outputConverter,
        ErrorCode errorCode,
        String errorPrefix,
        String logContext,
        Logger log
    ) {
        return invoke(chatClient, systemPromptWithFormat, userPrompt, outputConverter, null, errorCode, errorPrefix, logContext, log);
    }

    public <T> T invoke(
        ChatClient chatClient,
        String systemPromptWithFormat,
        String userPrompt,
        BeanOutputConverter<T> outputConverter,
        Class<T> targetClass,
        ErrorCode errorCode,
        String errorPrefix,
        String logContext,
        Logger log
    ) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String attemptSystemPrompt = attempt == 1
                ? systemPromptWithFormat
                : buildRetrySystemPrompt(systemPromptWithFormat, lastError);
            try {
                T result = chatClient.prompt()
                    .system(attemptSystemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(outputConverter);
                return result;
            } catch (Exception e) {
                lastError = e;
                log.warn("{}结构化解析失败，准备重试: attempt={}, error={}", logContext, attempt, e.getMessage());
                
                if (targetClass != null && attempt < maxAttempts) {
                    try {
                        String rawResponse = chatClient.prompt()
                            .system(attemptSystemPrompt)
                            .user(userPrompt)
                            .call()
                            .content();
                        String cleanedJson = cleanAndRepairJson(rawResponse, log, logContext);
                        return objectMapper.readValue(cleanedJson, targetClass);
                    } catch (Exception repairEx) {
                        log.warn("{}JSON修复后解析仍失败: {}", logContext, repairEx.getMessage());
                    }
                }
            }
        }

        throw new BusinessException(
            errorCode,
            errorPrefix + (lastError != null ? lastError.getMessage() : "unknown")
        );
    }

    private String cleanAndRepairJson(String rawResponse, Logger log, String logContext) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return rawResponse;
        }

        String cleaned = rawResponse.trim();
        
        cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            cleaned = matcher.group();
        }
        
        cleaned = TRAILING_COMMA_PATTERN.matcher(cleaned).replaceAll("$1");
        
        cleaned = attemptRepairUnclosedStructures(cleaned);
        
        log.debug("{} JSON清理完成，原始长度: {}, 清理后长度: {}", logContext, rawResponse.length(), cleaned.length());
        return cleaned;
    }

    private String attemptRepairUnclosedStructures(String json) {
        int openBraces = 0;
        int openBrackets = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') openBraces++;
                else if (c == '}') openBraces--;
                else if (c == '[') openBrackets++;
                else if (c == ']') openBrackets--;
            }
        }
        
        StringBuilder repaired = new StringBuilder(json);
        
        while (openBrackets > 0) {
            repaired.append("]");
            openBrackets--;
        }
        
        while (openBraces > 0) {
            repaired.append("}");
            openBraces--;
        }
        
        return repaired.toString();
    }

    private String buildRetrySystemPrompt(String systemPromptWithFormat, Exception lastError) {
        StringBuilder prompt = new StringBuilder(systemPromptWithFormat)
            .append("\n\n")
            .append(STRICT_JSON_INSTRUCTION)
            .append("\n上次输出解析失败，请仅返回合法 JSON。");

        if (includeLastErrorInRetryPrompt && lastError != null && lastError.getMessage() != null) {
            prompt.append("\n上次失败原因：")
                .append(sanitizeErrorMessage(lastError.getMessage()));
        }
        return prompt.toString();
    }

    private String sanitizeErrorMessage(String message) {
        String oneLine = message.replace('\n', ' ').replace('\r', ' ').trim();
        if (oneLine.length() > 200) {
            return oneLine.substring(0, 200) + "...";
        }
        return oneLine;
    }
}
