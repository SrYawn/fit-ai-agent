package com.zsr.fitaiagent.controller;

import com.zsr.fitaiagent.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@Tag(name = "会话管理", description = "聊天会话与消息历史接口")
@Slf4j
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @GetMapping("/sessions")
    @Operation(summary = "查询用户会话列表")
    public ResponseEntity<Map<String, Object>> listSessions(
            @Parameter(description = "用户ID") @RequestParam Long userId
    ) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", chatSessionService.listSessions(userId)
            ));
        } catch (Exception e) {
            log.error("查询会话列表失败: userId={}", userId, e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "查询会话列表失败");
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "查询会话消息列表")
    public ResponseEntity<Map<String, Object>> listMessages(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @Parameter(description = "用户ID") @RequestParam Long userId
    ) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", chatSessionService.listMessages(userId, sessionId)
            ));
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("查询会话消息失败: userId={}, sessionId={}", userId, sessionId, e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "查询会话消息失败");
        }
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "success", false,
                "message", message
        ));
    }
}
