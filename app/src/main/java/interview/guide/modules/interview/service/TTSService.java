package interview.guide.modules.interview.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TTSService {

    private final RestClient restClient;
    private final String model;
    private final String defaultVoice;
    private final boolean enabled;
    private final String endpoint;
    private final WebClient webClient;
    /*public TTSService(
            @Value("${app.tts.api-key}") String apiKey,
            @Value("${app.tts.endpoint}") String endpoint,
            @Value("${app.tts.model}") String model,
            @Value("${app.tts.voice}") String defaultVoice,
            @Value("${app.tts.enabled:true}") boolean enabled,
    @Value("${app.tts.connect-timeout:30000}") int connectTimeout,
    @Value("${app.tts.read-timeout:120000}") int readTimeout) {

        this.endpoint = endpoint;
        this.model = model;
        this.defaultVoice = defaultVoice;
        this.enabled = enabled;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        // 构建 REST 客户端（兼容 SiliconFlow / OpenAI TTS API）
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
        log.info("TTS 服务初始化完成：endpoint={}, model={}, voice={}, enabled={}, connectTimeout={}ms, readTimeout={}ms",
                endpoint, model, defaultVoice, enabled, connectTimeout, readTimeout);
    }*/

    public TTSService(
            @Value("${app.tts.api-key}") String apiKey,
            @Value("${app.tts.endpoint}") String endpoint,
            @Value("${app.tts.model}") String model,
            @Value("${app.tts.voice}") String defaultVoice,
            @Value("${app.tts.enabled:true}") boolean enabled,
            @Value("${app.tts.connect-timeout:30000}") int connectTimeout,
            @Value("${app.tts.read-timeout:120000}") int readTimeout) {

        this.endpoint = endpoint;
        this.model = model;
        this.defaultVoice = defaultVoice;
        this.enabled = enabled;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        // 构建 REST 客户端（兼容 SiliconFlow / OpenAI TTS API）
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();

        // 构建 WebClient 用于流式 TTS
        this.webClient = WebClient.builder()
                .baseUrl(endpoint)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("TTS 服务初始化完成：endpoint={}, model={}, voice={}, enabled={}, connectTimeout={}ms, readTimeout={}ms",
                endpoint, model, defaultVoice, enabled, connectTimeout, readTimeout);
    }

    /**
     * 流式生成语音（SSE）- 实时返回音频块
     *
     * @param text 要转换的文本
     * @param voice 声音名称（可选）
     * @param rate 语速倍率（可选）
     * @return 音频字节流的 Flux
     */
    public Flux<byte[]> generateSpeechStream(String text, String voice, Double rate) {
        if (!enabled) {
            log.warn("TTS 服务未启用");
            return Flux.empty();
        }

        if (text == null || text.trim().isEmpty()) {
            log.warn("输入文本为空");
            return Flux.empty();
        }

        String selectedVoice = (voice != null && !voice.isBlank()) ? voice : defaultVoice;
        double selectedRate = (rate != null) ? rate : 1.0;

        log.info("开始流式 TTS：文本长度={}, 声音={}, 语速={}", text.length(), selectedVoice, selectedRate);

        try {
            // 说明：
            // 不同厂商的 OpenAI 兼容 TTS 接口对“是否真正流式返回二进制”支持不一致。
            // 为保证你的 WebSocket TTS 一定能“分块播放”，这里改为：先生成完整 mp3，再切分为小块 Flux 推送。
            return Mono.fromCallable(() -> generateSpeech(text, selectedVoice, selectedRate))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(audio -> Flux.fromIterable(chunkBytes(audio, 16 * 1024)))
                    // 控制推送节奏，避免一次性把 WS 缓冲打爆；可按需要调小/调大
                    .delayElements(Duration.ofMillis(10))
                    .doOnNext(chunk -> log.debug("发送音频块：{} bytes", chunk.length))
                    .doOnComplete(() -> log.info("流式 TTS 完成"))
                    .doOnError(e -> log.error("流式 TTS 失败：{}", e.getMessage(), e));

        } catch (Exception e) {
            log.error("流式 TTS 失败：text={}, error={}", text, e.getMessage(), e);
            return Flux.error(new RuntimeException("流式 TTS 失败：" + e.getMessage(), e));
        }
    }

    /**
     * 流式生成语音（使用默认配置）
     */
    public Flux<byte[]> generateSpeechStream(String text) {
        return generateSpeechStream(text, null, null);
    }

    private ClientHttpRequestFactory getRequestFactory(int connectTimeout, int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    private List<byte[]> chunkBytes(byte[] data, int chunkSize) {
        if (data == null || data.length == 0) {
            return List.of();
        }
        int size = Math.max(1, chunkSize);
        List<byte[]> chunks = new ArrayList<>((data.length + size - 1) / size);
        for (int offset = 0; offset < data.length; offset += size) {
            int end = Math.min(data.length, offset + size);
            byte[] chunk = new byte[end - offset];
            System.arraycopy(data, offset, chunk, 0, chunk.length);
            chunks.add(chunk);
        }
        return chunks;
    }
    /**
     * 生成语音（MP3 格式）- 使用 REST API 直接调用
     *
     * @param text 要转换的文本
     * @param voice 声音名称（可选，默认使用配置文件中的声音）
     * @param rate 语速倍率（可选，1.0 为正常语速，范围 0.25-4.0）
     * @return MP3 音频字节数组
     */
    public byte[] generateSpeech(String text, String voice, Double rate) {
        if (!enabled) {
            log.warn("TTS 服务未启用，返回空音频");
            return new byte[0];
        }

        if (text == null || text.trim().isEmpty()) {
            log.warn("输入文本为空，返回空音频");
            return new byte[0];
        }

        String selectedVoice = (voice != null && !voice.isBlank()) ? voice : defaultVoice;
        double selectedRate = (rate != null) ? rate : 1.0;

        log.info("开始生成 TTS 语音：文本长度={}, 声音={}, 语速={}",
                text.length(), selectedVoice, selectedRate);

        try {
            // 构建请求体（兼容 OpenAI TTS API 格式）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", text);
            requestBody.put("voice", "FunAudioLLM/CosyVoice2-0.5B:"+selectedVoice);
            requestBody.put("response_format", "mp3");

            // 如果语速不是默认值，添加到请求中
            if (selectedRate != 1.0) {
                requestBody.put("speed", selectedRate);
            }

            // 调用 TTS API 获取音频数据
            byte[] audioData = restClient.post()
                    .uri(endpoint)
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);

            log.info("TTS 语音生成成功：音频大小={} bytes", audioData.length);
            return audioData;

        } catch (Exception e) {
            log.error("TTS 语音生成失败：text={}, voice={}, error={}",
                    text, selectedVoice, e.getMessage(), e);
            throw new RuntimeException("TTS 语音生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 生成语音（使用默认配置）
     *
     * @param text 要转换的文本
     * @return MP3 音频字节数组
     */
    public byte[] generateSpeech(String text) {
        return generateSpeech(text, null, null);
    }

    /**
     * 检查 TTS 服务是否可用
     */
    public boolean isEnabled() {
        return enabled;
    }
}
