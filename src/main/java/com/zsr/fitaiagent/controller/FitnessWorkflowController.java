package com.zsr.fitaiagent.controller;

import com.zsr.fitaiagent.graph.FitnessWorkflowGraph;
import com.zsr.fitaiagent.graph.FitnessWorkflowGraph.WorkflowExecutionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 健身 AI Agent 工作流控制器
 */
@RestController
@RequestMapping("/api/fitness/workflow")
@Tag(name = "健身工作流", description = "基于多 Agent 编排的健身 AI 工作流")
@Slf4j
public class FitnessWorkflowController {

    @Autowired
    private FitnessWorkflowGraph fitnessWorkflowGraph;

    @PostMapping("/execute")
    @Operation(summary = "执行健身工作流", description = "根据用户输入自动识别意图并执行相应的 Agent 流程")
    public WorkflowExecutionResponse executeWorkflow(
            @Parameter(description = "用户输入") @RequestParam String userInput,
            @Parameter(description = "用户ID（可选，默认为1）") @RequestParam(required = false) Long userId,
            @Parameter(description = "会话ID（可选，首次请求不传则自动生成）") @RequestParam(required = false) String sessionId
    ) {
        String resolvedSessionId = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
        log.info("收到工作流执行请求 - 用户输入: {}, 用户ID: {}, sessionId: {}", userInput, userId, resolvedSessionId);
        return fitnessWorkflowGraph.executeWorkflow(userInput, userId, resolvedSessionId);
    }

    @GetMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式执行健身工作流", description = "以 SSE 方式返回 token 级增量文本，并在 payload 中附带节点元数据")
    public SseEmitter executeWorkflowStream(
            @Parameter(description = "用户输入") @RequestParam String userInput,
            @Parameter(description = "用户ID（可选，默认为1）") @RequestParam(required = false) Long userId,
            @Parameter(description = "会话ID（可选，首次请求不传则自动生成）") @RequestParam(required = false) String sessionId
    ) {
        String resolvedSessionId = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
        log.info("收到流式工作流执行请求 - 用户输入: {}, 用户ID: {}, sessionId: {}", userInput, userId, resolvedSessionId);

        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        CompletableFuture.runAsync(() -> {
            try {
                fitnessWorkflowGraph.executeWorkflowStream(userInput, userId, resolvedSessionId, text -> {
                    try {
                        emitter.send(
                                SseEmitter.event()
                                        .name(text.event())
                                        .data(text.toPayload())
                        );
                    } catch (IOException e) {
                        log.error("发送 SSE 数据失败", e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("流式执行工作流时出错", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
