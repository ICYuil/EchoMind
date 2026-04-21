package interview.guide.common;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

@ControllerAdvice
public class ColorResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 对所有返回 JSON 的接口生效
        return true; 
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        
        String colorId = ColorContextHolder.getColorId();
        
        if (colorId != null && body != null) {
            // 情况 A：如果你有统一的 Result 包装类，且该类有扩展字段映射
            if (body instanceof Map) {
                ((Map<String, Object>) body).put("colorId", colorId);
            } 
            // 情况 B：如果 body 是你的 Result 对象
            // 假设你的 Result 类有个 setExtra(Map) 或者直接反射注入
            else {
                // 如果不想改 Result 类，可以把 body 包装进一个新 Map 返回
                // 但这可能会改变前端接收的结构，需谨慎
                // 推荐做法：在统一的 Result 父类里留一个 @JsonAnyGetter 字段
            }
        }
        
        return body;
    }
}