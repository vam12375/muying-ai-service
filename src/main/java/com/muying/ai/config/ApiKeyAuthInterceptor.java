package com.muying.ai.config;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简单 API Key 鉴权拦截器
 * 当前仅用于保护知识库上传/删除等高风险接口
 */
@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";
    private static final String DEFAULT_PLACEHOLDER_KEY = "change-me";

    private final SecurityProperties securityProperties;

    public ApiKeyAuthInterceptor(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!securityProperties.isEnabled()) {
            return true;
        }

        String configuredKey = securityProperties.getKey();
        if (configuredKey == null || configuredKey.isBlank() || DEFAULT_PLACEHOLDER_KEY.equals(configuredKey)) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "API_KEY_NOT_CONFIGURED", "服务端未配置有效的 API Key", request.getRequestURI());
            return false;
        }

        String requestApiKey = request.getHeader(API_KEY_HEADER);
        if (requestApiKey != null && requestApiKey.equals(configuredKey)) {
            return true;
        }

        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED", "未授权访问受保护接口", request.getRequestURI());
        return false;
    }

    private void writeJson(HttpServletResponse response,
            int status,
            String code,
            String message,
            String path) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("path", path);
        body.put("timestamp", OffsetDateTime.now().toString());
        response.getWriter().write(JSON.toJSONString(body));
    }
}
