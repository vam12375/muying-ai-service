package com.muying.ai.controller;

import com.muying.ai.model.KnowledgeDocument;
import com.muying.ai.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 * 提供文档上传、列表、删除接口
 */
@Tag(name = "知识库管理", description = "文档上传 / 列表查询 / 删除")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Operation(summary = "上传知识文档", description = "支持 TXT、MD 格式，自动切片并嵌入到 Milvus 向量库")
    @PostMapping("/upload")
    public KnowledgeDocument upload(
            @Parameter(description = "文档文件（TXT/MD）") @RequestParam("file") MultipartFile file,
            @Parameter(description = "分类：faq / policy / parenting / promotion") @RequestParam(defaultValue = "faq") String category
    ) throws IOException {
        return knowledgeService.uploadDocument(file, category);
    }

    @Operation(summary = "文档列表", description = "按上传时间倒序排列")
    @GetMapping("/list")
    public List<KnowledgeDocument> list() {
        return knowledgeService.listDocuments();
    }

    @Operation(summary = "删除文档", description = "同时删除 Milvus 向量和 Redis 元数据")
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@Parameter(description = "文档 ID") @PathVariable String id) {
        knowledgeService.deleteDocument(id);
        return Map.of("message", "删除成功");
    }
}
