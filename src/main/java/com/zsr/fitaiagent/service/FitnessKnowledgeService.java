package com.zsr.fitaiagent.service;

import com.zsr.fitaiagent.rag.FitnessDocumentLoader;
import com.zsr.fitaiagent.rag.MyTokenTextSplitter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 健身知识库业务逻辑
 */
@Service
@Slf4j
public class FitnessKnowledgeService {

    private static final int DASHSCOPE_MAX_EMBEDDING_BATCH_SIZE = 10;

    /**
     * DashScope Embedding API 单次请求 input.contents 最大数量。
     * 默认值为 10，可通过配置 fitness.knowledge.embedding-batch-size 覆盖。
     */
    @Value("${fitness.knowledge.embedding-batch-size:10}")
    private int embeddingBatchSize;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private FitnessDocumentLoader fitnessDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    /**
     * 初始化知识库：读取所有本地 md 文档，切分后写入 ES 向量库
     *
     * @return 导入的文档片段数量
     */
    public int initKnowledgeBase() {
        log.info("开始初始化健身知识库，embedding 批大小: {}", embeddingBatchSize);
        // 1. 加载所有 Markdown 文档
        List<Document> documents = fitnessDocumentLoader.loadMarkdowns();
        if (documents.isEmpty()) {
            log.warn("未找到任何健身知识文档");
            return 0;
        }
        // 2. 使用 TokenTextSplitter 切分文档
        List<Document> splitDocuments = myTokenTextSplitter.splitDocuments(documents);
        log.info("文档切分完成，共 {} 个片段", splitDocuments.size());
        // 3. 分批写入 ES 向量库
        batchAddDocuments(splitDocuments);
        log.info("健身知识库初始化完成，共导入 {} 个文档片段", splitDocuments.size());
        return splitDocuments.size();
    }

    /**
     * 上传单个 md 文件并写入 ES 向量库
     *
     * @param file     上传的 Markdown 文件
     * @param category 知识分类
     * @return 导入的文档片段数量
     */
    public int uploadDocument(MultipartFile file, String category) throws IOException {
        log.info("上传文档: {}, 分类: {}", file.getOriginalFilename(), category);
        // 1. 将 MultipartFile 转为 Resource 并加载
        org.springframework.core.io.Resource resource = new InputStreamResource(file.getInputStream()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        List<Document> documents = fitnessDocumentLoader.loadSingleMarkdown(resource, category);
        // 2. 切分文档
        List<Document> splitDocuments = myTokenTextSplitter.splitDocuments(documents);
        log.info("上传文档切分完成，共 {} 个片段", splitDocuments.size());
        // 3. 分批写入 ES 向量库
        batchAddDocuments(splitDocuments);
        log.info("文档上传并导入完成: {}", file.getOriginalFilename());
        return splitDocuments.size();
    }

    /**
     * 分批将文档写入向量库
     */
    private void batchAddDocuments(List<Document> documents) {
        int currentBatchSize = resolveEmbeddingBatchSize();
        for (int i = 0; i < documents.size(); i += currentBatchSize) {
            int end = Math.min(i + currentBatchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            log.info("写入向量库：批次 {}/{}, 本批 {} 条",
                    (i / currentBatchSize) + 1,
                    (int) Math.ceil((double) documents.size() / currentBatchSize),
                    batch.size());
            vectorStore.add(batch);
        }
    }

    private int resolveEmbeddingBatchSize() {
        if (embeddingBatchSize <= 0) {
            log.warn("配置 fitness.knowledge.embedding-batch-size={} 非法，自动回退为 {}",
                    embeddingBatchSize, DASHSCOPE_MAX_EMBEDDING_BATCH_SIZE);
            return DASHSCOPE_MAX_EMBEDDING_BATCH_SIZE;
        }
        if (embeddingBatchSize > DASHSCOPE_MAX_EMBEDDING_BATCH_SIZE) {
            log.warn("配置 fitness.knowledge.embedding-batch-size={} 超出 DashScope 上限，自动裁剪为 {}",
                    embeddingBatchSize, DASHSCOPE_MAX_EMBEDDING_BATCH_SIZE);
            return DASHSCOPE_MAX_EMBEDDING_BATCH_SIZE;
        }
        return embeddingBatchSize;
    }

    /**
     * 检索知识库
     *
     * @param query    查询内容
     * @param category 分类过滤（可选）
     * @return 检索到的文档列表
     */
    public List<Document> searchKnowledge(String query, String category) {
        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.5);
        // 可选按 category 元数据过滤
        if (category != null && !category.isBlank()) {
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            searchRequestBuilder.filterExpression(builder.eq("category", category).build());
        }
        List<Document> results = vectorStore.similaritySearch(searchRequestBuilder.build());
        log.info("知识库检索完成，query: {}, category: {}, 结果数: {}", query, category, results.size());
        return results;
    }
}
