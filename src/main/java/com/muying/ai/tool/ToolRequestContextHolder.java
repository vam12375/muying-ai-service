package com.muying.ai.tool;

import com.muying.ai.model.ChatRequest;

import java.util.Locale;

/**
 * Function Calling 工具请求上下文持有器
 * 用于把当前聊天请求中的身份/场景信息传递给后端工具层做权限校验。
 */
public final class ToolRequestContextHolder {

    private static final String DEFAULT_LOGIN_STATUS = "ANONYMOUS";
    private static final String DEFAULT_SCENARIO = "GENERAL_ASSISTANT";
    private static final InheritableThreadLocal<ToolRequestContext> CONTEXT = new InheritableThreadLocal<>();

    private ToolRequestContextHolder() {
    }

    public static void set(ChatRequest request) {
        if (request == null) {
            clear();
            return;
        }
        CONTEXT.set(new ToolRequestContext(
                normalize(request.getUserId()),
                normalizeUpper(request.getLoginStatus(), DEFAULT_LOGIN_STATUS),
                normalizeUpper(request.getScenario(), DEFAULT_SCENARIO)));
    }

    public static ToolRequestContext get() {
        ToolRequestContext context = CONTEXT.get();
        return context == null ? ToolRequestContext.anonymous() : context;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record ToolRequestContext(String userId, String loginStatus, String scenario) {

        public static ToolRequestContext anonymous() {
            return new ToolRequestContext("", DEFAULT_LOGIN_STATUS, DEFAULT_SCENARIO);
        }

        public boolean hasUserId() {
            return userId != null && !userId.isBlank();
        }

        public boolean isLoggedIn() {
            return "LOGGED_IN".equals(loginStatus)
                    || "AUTHENTICATED".equals(loginStatus)
                    || "SIGNED_IN".equals(loginStatus)
                    || "LOGIN".equals(loginStatus);
        }

        public boolean matchesScenario(String... allowedScenarios) {
            if (allowedScenarios == null || allowedScenarios.length == 0) {
                return true;
            }
            for (String allowedScenario : allowedScenarios) {
                if (allowedScenario != null && allowedScenario.equalsIgnoreCase(scenario)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeUpper(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized.isBlank() ? defaultValue : normalized.toUpperCase(Locale.ROOT);
    }
}
