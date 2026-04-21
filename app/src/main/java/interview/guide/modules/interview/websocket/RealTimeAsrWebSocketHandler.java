package interview.guide.modules.interview.websocket;



import com.google.gson.Gson;
import interview.guide.modules.interview.pojo.AsrAnalysisResult;
import interview.guide.modules.interview.service.AsrAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RealTimeAsrWebSocketHandler extends TextWebSocketHandler {

    private final AsrAnalysisService asrAnalysisService;
    private final Gson gson;
    
    // 存储活跃的 WebSocket 会话
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // 存储每个会话的音频数据缓冲区
    private final Map<String, java.io.ByteArrayOutputStream> audioBuffers = new ConcurrentHashMap<>();

    // 简单防抖：同一会话同一时刻只跑一个增量识别任务
    private final Map<String, AtomicBoolean> incrementalRunning = new ConcurrentHashMap<>();

    public RealTimeAsrWebSocketHandler(AsrAnalysisService asrAnalysisService, Gson gson) {
        this.asrAnalysisService = asrAnalysisService;
        this.gson = gson;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        audioBuffers.put(sessionId, new java.io.ByteArrayOutputStream());
        incrementalRunning.put(sessionId, new AtomicBoolean(false));
        
        log.info("ASR WebSocket 连接建立：{}", sessionId);
        
        // 发送连接确认消息
        sendMessage(session, Map.of(
            "type", "connected",
            "sessionId", sessionId,
            "message", "ASR 服务已连接，请发送音频数据"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到 ASR 消息：{}", payload);
        
        try {
            // 解析 JSON 消息
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = gson.fromJson(payload, Map.class);
            String messageType = (String) msg.get("type");
            
            if ("audio_chunk".equals(messageType)) {
                // 处理音频数据块
                handleAudioChunk(session, msg);
            } else if ("finalize".equals(messageType)) {
                // 处理最终识别请求
                handleFinalizeAsr(session);
            } else if ("ping".equals(messageType)) {
                // 心跳响应
                sendMessage(session, Map.of("type", "pong"));
            }
            
        } catch (Exception e) {
            log.error("处理 ASR 消息失败", e);
            sendMessage(session, Map.of(
                "type", "error",
                "message", "处理失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 处理音频数据块
     */
    private void handleAudioChunk(WebSocketSession session, Map<String, Object> msg) throws Exception {
        String base64Audio = (String) msg.get("data");
        if (base64Audio == null || base64Audio.isEmpty()) {
            return;
        }
        
        // 解码 Base64 音频数据
        byte[] audioData = Base64.getDecoder().decode(base64Audio);
        
        // 添加到缓冲区
        java.io.ByteArrayOutputStream buffer = audioBuffers.get(session.getId());
        if (buffer != null) {
            buffer.write(audioData);
            log.debug("收到音频块：{} bytes, 缓冲区大小：{} bytes", audioData.length, buffer.size());
        }

        // 可选：增量识别（不是“真流式 ASR”，而是基于累计音频做增量转写）
        // 客户端可在 audio_chunk 中传 auto_incremental=true 来开启
        boolean autoIncremental = msg.get("auto_incremental") != null && Boolean.TRUE.equals(msg.get("auto_incremental"));
        if (autoIncremental && buffer != null && buffer.size() >= 120_000) { // 约 1-2 秒音频（取决于采样率/编码），阈值可调
            maybeRunIncrementalAsr(session, buffer.toByteArray());
        }

        sendMessage(session, Map.of(
            "type", "chunk_received",
            "size", audioData.length
        ));
    }

    private void maybeRunIncrementalAsr(WebSocketSession session, byte[] audioSnapshot) {
        String sessionId = session.getId();
        AtomicBoolean flag = incrementalRunning.get(sessionId);
        if (flag == null || !flag.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                MultipartFile multipartFile = new SimpleMultipartFile(
                        "audio.wav",
                        "audio.wav",
                        "audio/wav",
                        audioSnapshot
                );
                AsrAnalysisResult result = asrAnalysisService.recognizeRealTime(multipartFile).get();
                sendMessage(session, Map.of(
                        "type", "partial_transcription",
                        "data", gson.toJsonTree(result)
                ));
            } catch (Exception e) {
                log.warn("增量 ASR 失败：{}", e.getMessage());
            } finally {
                flag.set(false);
            }
        });
    }

    /**
     * 处理最终识别请求
     */
    private void handleFinalizeAsr(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        java.io.ByteArrayOutputStream buffer = audioBuffers.get(sessionId);

        if (buffer == null || buffer.size() == 0) {
            sendMessage(session, Map.of(
                    "type", "error",
                    "message", "没有收到音频数据"
            ));
            return;
        }

        byte[] audioData = buffer.toByteArray();
        log.info("开始 ASR 识别，音频大小：{} bytes", audioData.length);

        // 异步执行 ASR 识别
        CompletableFuture.runAsync(() -> {
            try {
                // 使用自定义的 MultipartFile 实现，直接从内存数据创建
                MultipartFile multipartFile = new SimpleMultipartFile(
                        "audio.wav",
                        "audio.wav",
                        "audio/wav",
                        audioData
                );

                AsrAnalysisResult result = asrAnalysisService.recognizeRealTime(multipartFile).get();

                // 发送识别结果
                sendMessage(session, Map.of(
                        "type", "transcription_result",
                        "data", gson.toJsonTree(result)
                ));

                log.info("ASR 识别完成：{}", result.getTranscription());

                // 清空缓冲区
                buffer.reset();

            } catch (Exception e) {
                log.error("ASR 识别失败", e);
                try {
                    sendMessage(session, Map.of(
                            "type", "error",
                            "message", "识别失败：" + e.getMessage()
                    ));
                } catch (Exception ex) {
                    log.error("发送错误消息失败", ex);
                }
            }
        });
    }

    /**
     * 简单的 MultipartFile 实现
     */
    private static class SimpleMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public SimpleMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        audioBuffers.remove(sessionId);
        incrementalRunning.remove(sessionId);
        log.info("ASR WebSocket 连接关闭：{}", sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("ASR WebSocket 传输错误", exception);
        if (session.isOpen()) {
            sendMessage(session, Map.of(
                "type", "error",
                "message", "传输错误：" + exception.getMessage()
            ));
        }
    }

    /**
     * 发送消息到客户端
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> data) throws Exception {
        if (session.isOpen()) {
            String json = gson.toJson(data);
            session.sendMessage(new TextMessage(json));
        }
    }

    /**
     * 广播消息给所有连接的客户端
     */
    public void broadcastMessage(Map<String, Object> data) {
        String json = gson.toJson(data);
        activeSessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (Exception e) {
                log.error("广播消息失败", e);
            }
        });
    }
}
