package com.muying.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RAG 异常告警通知器
 * 当前通过结构化日志输出告警事件，便于后续接入监控平台采集
 */
@Slf4j
@Component
public class RagAlertNotifier {

    private final boolean enabled;

    public RagAlertNotifier(@Value("${rag.alert.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public void notifyRagDegraded(String userMessage, Exception exception) {
        if (!enabled) {
            return;
        }

        String sanitizedMessage = userMessage == null ? "" : userMessage.replaceAll("\\s+", " ").trim();
        if (sanitizedMessage.length() > 120) {
            sanitizedMessage = sanitizedMessage.substring(0, 120) + "...";
        }

        log.error("ALERT[RAG_DEGRADED] message='{}' reason='{}'", sanitizedMessage, exception.getMessage());
    }
}
