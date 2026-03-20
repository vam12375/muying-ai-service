package com.muying.ai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简单 API Key 鉴权拦截器
 * 当前仅用于保护知识库上传/删除等高风险接口
 */
@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";

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

        String requestApiKey = request.getHeader(API_KEY_HEADER);
        if (requestApiKey != null && requestApiKey.equals(securityProperties.getKey())) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"未授权访问知识库管理接口\"}");
        return false;
    }
}
