package com.muying.ai;

import com.muying.ai.model.KnowledgeDocument;
import com.muying.ai.service.KnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 启动时预加载 classpath:knowledge/** 下的内置知识文档
 * 已存在（按文件名去重）的文档跳过，避免重复入库
 */
@Slf4j
@Component
public class KnowledgeInitializer implements ApplicationRunner {

    private final KnowledgeService knowledgeService;
    private final ResourcePatternResolver resourcePatternResolver;

    public KnowledgeInitializer(KnowledgeService knowledgeService,
                                ResourcePatternResolver resourcePatternResolver) {
        this.knowledgeService = knowledgeService;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始预加载内置知识文档...");

        Set<String> existingFileNames = knowledgeService.listDocuments()
                .stream()
                .map(KnowledgeDocument::getFileName)
                .collect(Collectors.toSet());

        Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources("classpath:knowledge/**/*.md");
        } catch (IOException e) {
            log.warn("扫描知识文档目录失败，跳过预加载: {}", e.getMessage());
            return;
        }

        int loaded = 0;
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            if (fileName == null) {
                continue;
            }
            if (existingFileNames.contains(fileName)) {
                log.debug("知识文档已存在，跳过: {}", fileName);
                continue;
            }
            String category = extractCategory(resource);
            try (InputStream is = resource.getInputStream()) {
                knowledgeService.uploadDocument(is, fileName, category);
                loaded++;
            } catch (Exception e) {
                log.error("预加载知识文档失败: {} - {}", fileName, e.getMessage());
            }
        }

        log.info("内置知识文档预加载完成，本次新增 {} 篇", loaded);
    }

    /**
     * 从资源路径中提取分类名（取 knowledge/ 后的第一级目录名）
     */
    private String extractCategory(Resource resource) {
        try {
            String path = resource.getURL().getPath().replace("\\", "/");
            int idx = path.lastIndexOf("/knowledge/");
            if (idx >= 0) {
                String sub = path.substring(idx + "/knowledge/".length()); // e.g. "faq/奶粉冲泡指南.md"
                int slash = sub.indexOf('/');
                if (slash > 0) {
                    return sub.substring(0, slash);
                }
            }
        } catch (IOException e) {
            log.warn("提取文档分类失败，使用默认值 faq: {}", e.getMessage());
        }
        return "faq";
    }
}
