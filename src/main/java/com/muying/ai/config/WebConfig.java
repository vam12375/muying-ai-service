package com.muying.ai.config;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Web 配置：CORS 跨域支持
 */
@Configuration
@EnableConfigurationProperties({ SecurityProperties.class, RateLimitProperties.class, MallApiProperties.class })
public class WebConfig implements WebMvcConfigurer {

    @Value("${web.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    private final ApiKeyAuthInterceptor apiKeyAuthInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(ApiKeyAuthInterceptor apiKeyAuthInterceptor, RateLimitProperties rateLimitProperties) {
        this.apiKeyAuthInterceptor = apiKeyAuthInterceptor;
        this.rateLimitInterceptor = new RateLimitInterceptor(rateLimitProperties);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(parseAllowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor)
                .addPathPatterns("/api/knowledge/upload", "/api/knowledge/*")
                .excludePathPatterns("/api/knowledge/list");

        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/chat/**");
    }

    @Bean
    public RestClient mallRestClient(MallApiProperties mallApiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) mallApiProperties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) mallApiProperties.getReadTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(mallApiProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private String[] parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }
}

@Validated
@ConfigurationProperties(prefix = "mall.api")
class MallApiProperties {

    /**
     * 商城 API 基础地址
     */
    @NotBlank(message = "商城 API 地址不能为空")
    private String baseUrl = "http://localhost:8080/api";

    /**
     * 连接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /**
     * 读取超时时间
     */
    private Duration readTimeout = Duration.ofSeconds(5);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}

@Validated
@ConfigurationProperties(prefix = "security.rate-limit")
class RateLimitProperties {

    /**
     * 是否启用基础限流
     */
    private boolean enabled = false;

    /**
     * 固定时间窗口内允许的最大请求数
     */
    @Min(value = 1, message = "限流最大请求数必须大于 0")
    private int maxRequests = 30;

    /**
     * 限流窗口时长（秒）
     */
    @Min(value = 1, message = "限流窗口时长必须大于 0")
    private int windowSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}

class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    RateLimitInterceptor(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!properties.isEnabled()) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowMillis = properties.getWindowSeconds() * 1000L;
        String requestKey = resolveClientId(request) + ":" + request.getRequestURI();

        WindowCounter counter = counters.compute(requestKey, (key, existing) -> {
            if (existing == null || existing.isExpired(now, windowMillis)) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.requestCount().incrementAndGet();
            return existing;
        });

        int currentCount = counter.requestCount().get();
        long resetAt = counter.windowStartMillis() + windowMillis;
        response.setHeader("X-RateLimit-Limit", String.valueOf(properties.getMaxRequests()));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, properties.getMaxRequests() - currentCount)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetAt / 1000));

        if (currentCount <= properties.getMaxRequests()) {
            return true;
        }

        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(buildRateLimitBody(request)));
        return false;
    }

    private Map<String, Object> buildRateLimitBody(HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "RATE_LIMITED");
        body.put("message", "请求过于频繁，请稍后再试");
        body.put("path", request.getRequestURI());
        body.put("timestamp", OffsetDateTime.now().toString());
        return body;
    }

    private String resolveClientId(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record WindowCounter(long windowStartMillis, AtomicInteger requestCount) {
        private boolean isExpired(long now, long windowMillis) {
            return now - windowStartMillis >= windowMillis;
        }
    }
}
