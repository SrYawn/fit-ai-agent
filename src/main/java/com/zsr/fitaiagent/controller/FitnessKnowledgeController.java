package com.zsr.fitaiagent.controller;

import com.zsr.fitaiagent.service.FitnessKnowledgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健身知识库接口
 */
@RestController
@RequestMapping("/fitness/knowledge")
@Slf4j
public class FitnessKnowledgeController {

    @Resource
    private FitnessKnowledgeService fitnessKnowledgeService;

    /**
     * 初始化知识库：构建 ES 索引 + 导入所有本地 md 文档
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initKnowledgeBase() {
        try {
            log.info("收到知识库初始化请求");
            int count = fitnessKnowledgeService.initKnowledgeBase();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "健身知识库初始化完成");
            result.put("documentCount", count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("知识库初始化失败", e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "知识库初始化失败", e);
        }
    }

    /**
     * 上传 md 文件到知识库
     *
     * @param file     Markdown 文件
     * @param category 知识分类（exercise, injury-recovery, nutrition, training-plan, body-knowledge）
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {
        try {
            log.info("收到文档上传请求: file={}, category={}", file.getOriginalFilename(), category);
            int count = fitnessKnowledgeService.uploadDocument(file, category);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文档上传并导入成功");
            result.put("filename", file.getOriginalFilename());
            result.put("category", category);
            result.put("documentCount", count);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("文档读取失败: file={}", file.getOriginalFilename(), e);
            return errorResponse(HttpStatus.BAD_REQUEST, "文档读取失败", e);
        } catch (Exception e) {
            log.error("文档上传并导入失败: file={}", file.getOriginalFilename(), e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "文档上传并导入失败", e);
        }
    }

    /**
     * 检索知识库
     *
     * @param query    查询内容
     * @param category 分类过滤（可选）
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchKnowledge(
            @RequestParam("query") String query,
            @RequestParam(value = "category", required = false) String category) {
        try {
            log.info("收到知识库检索请求: query={}, category={}", query, category);
            List<Document> results = fitnessKnowledgeService.searchKnowledge(query, category);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("query", query);
            result.put("category", category);
            result.put("total", results.size());
            result.put("documents", results.stream().map(doc -> {
                Map<String, Object> docMap = new HashMap<>();
                docMap.put("id", doc.getId());
                docMap.put("content", doc.getText());
                docMap.put("metadata", doc.getMetadata());
                return docMap;
            }).toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("知识库检索失败: query={}, category={}", query, category, e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "知识库检索失败", e);
        }
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message, Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        result.put("error", e.getMessage());
        return ResponseEntity.status(status).body(result);
    }
}
