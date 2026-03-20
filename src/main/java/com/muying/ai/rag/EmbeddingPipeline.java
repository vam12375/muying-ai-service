package com.muying.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * RAG 文档处理管道（LangChain4j）
 * 负责文档解析 → 分段；Embedding 由 Spring AI VectorStore 内部完成
 */
@Slf4j
@Component
public class EmbeddingPipeline {

    private DocumentSplitter splitter;

    @PostConstruct
    public void init() {
        // 文档分段策略：每段 300 token，重叠 50 token
        this.splitter = DocumentSplitters.recursive(300, 50);
        log.info("EmbeddingPipeline initialized");
    }

    /**
     * 解析文档并分段
     * 使用 TextDocumentParser 处理 TXT/MD 文件
     */
    public List<TextSegment> parseAndSplit(InputStream inputStream, String fileName) {
        TextDocumentParser parser = new TextDocumentParser();
        Document document = parser.parse(inputStream);
        document.metadata().put("source", fileName);

        List<TextSegment> segments = splitter.split(document);
        log.info("文档 {} 分段完成，共 {} 段", fileName, segments.size());
        return segments;
    }
}
