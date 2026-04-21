package interview.guide.common.config;


import interview.guide.modules.interview.websocket.RealTimeAsrWebSocketHandler;
import interview.guide.modules.interview.websocket.RealTimeTtsWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealTimeAsrWebSocketHandler realTimeAsrWebSocketHandler;
    private final RealTimeTtsWebSocketHandler realTimeTtsWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册实时 ASR WebSocket 处理器
        registry.addHandler(realTimeAsrWebSocketHandler, "/ws/asr")
                .setAllowedOrigins("*");
        
        // 注册流式 TTS WebSocket 处理器
        registry.addHandler(realTimeTtsWebSocketHandler, "/ws/tts")
                .setAllowedOrigins("*");
    }
}
