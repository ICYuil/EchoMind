package interview.guide.modules.interview.websocket;

import com.google.gson.Gson;
import interview.guide.modules.interview.service.TTSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RealTimeTtsWebSocketHandler extends TextWebSocketHandler {

    private final TTSService ttsService;
    private final Gson gson;
    
    // 存储活跃的 WebSocket 会话
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public RealTimeTtsWebSocketHandler(TTSService ttsService, Gson gson) {
        this.ttsService = ttsService;
        this.gson = gson;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        
        log.info("TTS WebSocket 连接建立：{}", sessionId);
        
        // 发送连接确认消息
        sendMessage(session, Map.of(
            "type", "connected",
            "sessionId", sessionId,
            "message", "TTS 服务已连接，请发送文本"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到 TTS 消息：{}", payload);
        
        try {
            // 解析 JSON 消息
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = gson.fromJson(payload, Map.class);
            String messageType = (String) msg.get("type");
            
            if ("generate".equals(messageType)) {
                // 处理语音生成请求
                handleGenerateSpeech(session, msg);
            } else if ("ping".equals(messageType)) {
                // 心跳响应
                sendMessage(session, Map.of("type", "pong"));
            }
            
        } catch (Exception e) {
            log.error("处理 TTS 消息失败", e);
            sendMessage(session, Map.of(
                "type", "error",
                "message", "处理失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 处理语音生成请求（流式）
     */
    private void handleGenerateSpeech(WebSocketSession session, Map<String, Object> msg) {
        String text = (String) msg.get("text");
        String voice = (String) msg.get("voice");
        Double rate = null;
        
        if (msg.get("rate") != null) {
            rate = ((Number) msg.get("rate")).doubleValue();
        }
        
        if (text == null || text.trim().isEmpty()) {
            try {
                sendMessage(session, Map.of(
                    "type", "error",
                    "message", "文本不能为空"
                ));
            } catch (Exception e) {
                log.error("发送错误消息失败", e);
            }
            return;
        }
        
        log.info("开始流式 TTS：文本长度={}, 声音={}, 语速={}", 
            text.length(), voice, rate);
        
        // 使用流式 TTS 服务
        Flux<byte[]> speechStream = ttsService.generateSpeechStream(text, voice, rate);
        
        speechStream.subscribe(
            chunk -> {
                // 收到音频块，发送到客户端
                try {
                    session.sendMessage(new BinaryMessage(chunk));
                    log.debug("发送音频块：{} bytes", chunk.length);
                } catch (Exception e) {
                    log.error("发送音频块失败", e);
                }
            },
            error -> {
                // 流式处理失败
                log.error("流式 TTS 失败", error);
                try {
                    sendMessage(session, Map.of(
                        "type", "error",
                        "message", "生成失败：" + error.getMessage()
                    ));
                } catch (Exception e) {
                    log.error("发送错误消息失败", e);
                }
            },
            () -> {
                // 流式处理完成
                log.info("流式 TTS 完成");
                try {
                    sendMessage(session, Map.of(
                        "type", "complete",
                        "message", "语音生成完成"
                    ));
                } catch (Exception e) {
                    log.error("发送完成消息失败", e);
                }
            }
        );
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        activeSessions.remove(sessionId);
        log.info("TTS WebSocket 连接关闭：{}", sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("TTS WebSocket 传输错误", exception);
        if (session.isOpen()) {
            sendMessage(session, Map.of(
                "type", "error",
                "message", "传输错误：" + exception.getMessage()
            ));
        }
    }

    /**
     * 发送文本消息到客户端
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> data) throws Exception {
        if (session.isOpen()) {
            String json = gson.toJson(data);
            session.sendMessage(new TextMessage(json));
        }
    }
}
