package interview.guide.modules.interview.pojo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 百炼配置（适配SpringBoot 4配置绑定）
 */
@Data
@Component
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {
    private String apiKey;
    private Asr asr;

    @Data
    public static class Asr {
        private String model;
        private Boolean enableLid;
        private Boolean enableItn;
    }
}