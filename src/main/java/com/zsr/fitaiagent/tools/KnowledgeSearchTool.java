package com.zsr.fitaiagent.tools;

import com.zsr.fitaiagent.service.FitnessKnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 健身知识检索工具
 * 用于从向量数据库中检索相关的健身知识
 */
@Component
@Slf4j
public class KnowledgeSearchTool {

    @Autowired
    private FitnessKnowledgeService fitnessKnowledgeService;

    @Tool(description = """
            Search for fitness knowledge from the knowledge base.
            Use this tool to retrieve relevant fitness information, training methods, exercise guides, etc.

            Parameters:
            - query: The search query (e.g., "weight loss training", "muscle building exercises")
            - category: Optional category filter (e.g., "injury-recovery", "exercise", "nutrition", "training-plan", "body-knowledge", "motivation")

            Returns: Relevant fitness knowledge documents
            """)
    public String searchKnowledge(String query, String category) {
        log.info("搜索健身知识 - query: {}, category: {}", query, category);

        try {
            List<Document> documents = fitnessKnowledgeService.searchKnowledge(query, category);

            if (documents.isEmpty()) {
                return "未找到相关的健身知识，请尝试使用其他关键词搜索。";
            }

            // 格式化返回结果
            String result = documents.stream()
                    .map(doc -> {
                        String content = doc.getText();
                        String docCategory = doc.getMetadata().getOrDefault("category", "未分类").toString();
                        return String.format("【分类：%s】\n%s", docCategory, content);
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));

            log.info("检索到 {} 条相关知识", documents.size());
            return result;

        } catch (Exception e) {
            log.error("检索健身知识时出错", e);
            return "检索知识时出错：" + e.getMessage();
        }
    }
}
