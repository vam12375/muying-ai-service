package com.muying.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 简单接口安全配置
 */
@ConfigurationProperties(prefix = "security.api-key")
public class SecurityProperties {

    /**
     * 是否启用 API Key 鉴权
     */
    private boolean enabled = false;

    /**
     * 管理接口访问密钥
     */
    private String key = "change-me";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
