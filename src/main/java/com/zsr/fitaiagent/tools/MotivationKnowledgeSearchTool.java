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
 * 激励心理学知识检索工具
 * 从向量数据库中检索 motivation 分类的运动心理学和激励策略知识
 */
@Component
@Slf4j
public class MotivationKnowledgeSearchTool {

    @Autowired
    private FitnessKnowledgeService fitnessKnowledgeService;

    @Tool(description = """
            Search for motivation and sports psychology knowledge from the knowledge base.
            Use this tool to retrieve strategies for encouraging users, handling frustration,
            positive reinforcement techniques, emotion management, and habit formation.

            Parameters:
            - query: The search query about motivation or psychology (e.g., "how to deal with training fatigue",
              "positive reinforcement strategies", "plateau period motivation")

            Returns: Relevant motivation and psychology knowledge documents
            """)
    public String searchMotivationKnowledge(String query) {
        log.info("搜索激励心理学知识 - query: {}", query);

        try {
            List<Document> documents = fitnessKnowledgeService.searchKnowledge(query, "motivation");

            if (documents.isEmpty()) {
                return "未找到相关的激励心理学知识，请尝试使用其他关键词搜索。";
            }

            String result = documents.stream()
                    .map(doc -> doc.getText())
                    .collect(Collectors.joining("\n\n---\n\n"));

            log.info("检索到 {} 条激励心理学知识", documents.size());
            return result;

        } catch (Exception e) {
            log.error("检索激励心理学知识时出错", e);
            return "检索知识时出错：" + e.getMessage();
        }
    }
}
