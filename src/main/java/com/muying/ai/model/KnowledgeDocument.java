package com.muying.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库文档元数据
 */
@Data
@Schema(description = "知识库文档元数据")
public class KnowledgeDocument {
    @Schema(description = "文档唯一ID")
    private String id;
    @Schema(description = "原始文件名", example = "退换货政策.md")
    private String fileName;
    @Schema(description = "分类", example = "policy", allowableValues = {"faq", "policy", "parenting", "promotion"})
    private String category;
    @Schema(description = "切片数量")
    private int segmentCount;
    @Schema(description = "上传时间（毫秒时间戳）")
    private long createdAt;
}
