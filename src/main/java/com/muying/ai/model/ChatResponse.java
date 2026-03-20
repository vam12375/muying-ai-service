package com.muying.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天响应（用于 SSE 流式事件）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /** 消息类型：token / tool_call / tool_result / done */
    private String type;
    /** 内容 */
    private String content;
    /** 工具名称（tool_call 类型时） */
    private String toolName;
}
