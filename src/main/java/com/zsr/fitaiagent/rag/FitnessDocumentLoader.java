package com.zsr.fitaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 健身知识文档加载器
 * 遍历 classpath:fitness-docs/ 下的子目录，根据子目录名自动附加 category 元数据
 */
@Component
@Slf4j
public class FitnessDocumentLoader {

    /**
     * 健身知识分类目录
     */
    private static final String[] CATEGORIES = {
            "exercise",
            "injury-recovery",
            "nutrition",
            "training-plan",
            "body-knowledge"
    };

    private final ResourcePatternResolver resourcePatternResolver;

    public FitnessDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有健身知识 Markdown 文档
     *
     * @return 所有文档列表（附带 category 和 filename 元数据）
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        for (String category : CATEGORIES) {
            try {
                Resource[] resources = resourcePatternResolver.getResources(
                        "classpath:fitness-docs/" + category + "/*.md");
                for (Resource resource : resources) {
                    List<Document> docs = loadSingleMarkdown(resource, category);
                    allDocuments.addAll(docs);
                }
            } catch (IOException e) {
                log.error("加载健身文档失败，分类: {}", category, e);
            }
        }
        log.info("健身知识文档加载完成，共加载 {} 个文档片段", allDocuments.size());
        return allDocuments;
    }

    /**
     * 加载单个 Markdown 文档
     *
     * @param resource Markdown 资源
     * @param category 分类名称
     * @return 文档列表
     */
    public List<Document> loadSingleMarkdown(Resource resource, String category) {
        String filename = resource.getFilename();
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", filename)
                .withAdditionalMetadata("category", category)
                .build();
        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
        List<Document> docs = markdownDocumentReader.get();
        log.info("加载文档: {} (分类: {}), 共 {} 个片段", filename, category, docs.size());
        return docs;
    }
}
