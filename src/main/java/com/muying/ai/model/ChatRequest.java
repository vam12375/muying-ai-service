package com.muying.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求
 */
@Data
@Schema(description = "聊天请求")
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    @Size(max = 4000, message = "消息长度不能超过 4000 字")
    @Schema(description = "用户消息", example = "婴儿多大可以吃辅食？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "会话ID（用于多轮对话记忆）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String sessionId;

    @Schema(description = "模型名称", example = "deepseek", allowableValues = {"deepseek", "qianwen", "zhipu"})
    private String model = "deepseek";
}
