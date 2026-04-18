package com.zsr.fitaiagent.graph;

import com.zsr.fitaiagent.agent.ActionGuidanceAgent;
import com.zsr.fitaiagent.agent.CompanionMotivationAgent;
import com.zsr.fitaiagent.agent.IntentRecognitionAgent;
import com.zsr.fitaiagent.agent.PlanGenerationAgent;
import com.zsr.fitaiagent.agent.UserProfileAgent;
import com.zsr.fitaiagent.chatmemory.InMemorySessionChatMemory;
import com.zsr.fitaiagent.tools.EmotionDetectionTool;
import com.zsr.fitaiagent.tools.IntentRouteTool;
import com.zsr.fitaiagent.tools.KnowledgeSearchTool;
import com.zsr.fitaiagent.tools.MotivationKnowledgeSearchTool;
import com.zsr.fitaiagent.tools.TerminateTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 健身工作流编排。
 * 同步接口返回结构化 JSON，流式接口返回 token 级 SSE 事件，并在 payload 中携带节点元数据。
 */
@Component
@Slf4j
public class FitnessWorkflowGraph {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private KnowledgeSearchTool knowledgeSearchTool;

    @Autowired
    private MotivationKnowledgeSearchTool motivationKnowledgeSearchTool;

    @Autowired
    private EmotionDetectionTool emotionDetectionTool;

    @Autowired
    private ChatMemory sessionChatMemory;

    @Autowired
    private ToolCallback[] mcpTools;

    public WorkflowExecutionResponse executeWorkflow(String userInput, Long userId, String sessionId) {
        Long resolvedUserId = userId != null ? userId : 1L;
        String resolvedSessionId = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
        log.info("执行工作流 - 用户输入: {}, 用户ID: {}, sessionId: {}", userInput, resolvedUserId, resolvedSessionId);

        try {
            AgentBundle agents = createAgentBundle();
            // 加载对话历史，注入意图识别上下文
            String sessionHistory = getSessionHistory(resolvedSessionId);
            String contextualInput = buildContextualInput(userInput, sessionHistory);
            String intent = recognizeIntent(contextualInput, agents);

            WorkflowExecutionResponse response = switch (intent) {
                case "plan_generation" -> executePlanWorkflow(contextualInput, resolvedUserId, resolvedSessionId, intent, agents);
                case "action_guidance" -> executeActionGuidanceWorkflow(contextualInput, resolvedUserId, resolvedSessionId, intent, agents);
                default -> executeChatWorkflow(userInput, resolvedUserId, resolvedSessionId, sessionHistory, intent, agents);
            };

            // 保存对话到 session 记忆
            saveToSessionMemory(resolvedSessionId, userInput, response.result());
            return response;
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            return WorkflowExecutionResponse.failed(userInput, resolvedUserId, resolvedSessionId, "workflow", "执行失败：" + e.getMessage());
        }
    }

    public void executeWorkflowStream(String userInput, Long userId, String sessionId, Consumer<WorkflowStreamEvent> outputConsumer) {
        Long resolvedUserId = userId != null ? userId : 1L;
        String resolvedSessionId = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
        AtomicLong sequence = new AtomicLong(0L);
        log.info("流式执行工作流 - 用户输入: {}, 用户ID: {}, sessionId: {}", userInput, resolvedUserId, resolvedSessionId);

        try {
            AgentBundle agents = createAgentBundle();
            // 加载对话历史
            String sessionHistory = getSessionHistory(resolvedSessionId);
            String contextualInput = buildContextualInput(userInput, sessionHistory);

            outputConsumer.accept(WorkflowStreamEvent.metadata(
                    "workflow",
                    "started",
                    "工作流开始执行",
                    null,
                    null,
                    resolvedSessionId,
                    sequence.incrementAndGet()
            ));

            String intent = recognizeIntent(contextualInput, agents);
            String executionNode = resolveExecutionNode(intent);
            outputConsumer.accept(WorkflowStreamEvent.metadata(
                    executionNode,
                    "routed",
                    "识别意图：" + intent,
                    intent,
                    null,
                    null,
                    sequence.incrementAndGet()
            ));

            String result = switch (intent) {
                case "plan_generation" -> {
                    streamPlanWorkflow(contextualInput, resolvedUserId, intent, agents, outputConsumer, sequence);
                    yield null; // streamPlanWorkflow handles its own output
                }
                case "action_guidance" -> {
                    streamActionGuidanceWorkflow(contextualInput, resolvedUserId, intent, agents, outputConsumer, sequence);
                    yield null;
                }
                default -> streamChatWorkflow(userInput, resolvedUserId, sessionHistory, intent, agents, outputConsumer, sequence);
            };

            // 保存对话到 session 记忆（流式场景下 result 可能为 null，由各子流程内部处理）
            if (result != null) {
                saveToSessionMemory(resolvedSessionId, userInput, result);
            }
        } catch (Exception e) {
            log.error("流式执行工作流失败", e);
            outputConsumer.accept(WorkflowStreamEvent.error(
                    "workflow",
                    "执行失败：" + e.getMessage(),
                    sequence.incrementAndGet()
            ));
        }
    }

    private WorkflowExecutionResponse executePlanWorkflow(String userInput, Long userId, String sessionId, String intent, AgentBundle agents) {
        String userProfile = agents.userProfileAgent().generateUserProfile(userId);
        String profileStatus = extractProfileStatus(userProfile);
        String result = agents.planAgent().generatePlan(userProfile, userInput);
        return WorkflowExecutionResponse.completed(userInput, userId, sessionId, intent, "plan_generation", result, profileStatus);
    }

    private WorkflowExecutionResponse executeActionGuidanceWorkflow(String userInput, Long userId, String sessionId, String intent, AgentBundle agents) {
        String result = agents.actionAgent().provideGuidance(userInput);
        return WorkflowExecutionResponse.completed(userInput, userId, sessionId, intent, "action_guidance", result, null);
    }

    private WorkflowExecutionResponse executeChatWorkflow(String userInput, Long userId, String sessionId, String sessionHistory, String intent, AgentBundle agents) {
        String result = agents.companionAgent().chat(userInput, userId, sessionHistory);
        return WorkflowExecutionResponse.completed(userInput, userId, sessionId, intent, "chat", result, null);
    }

    private void streamPlanWorkflow(String userInput,
                                    Long userId,
                                    String intent,
                                    AgentBundle agents,
                                    Consumer<WorkflowStreamEvent> outputConsumer,
                                    AtomicLong sequence) {
        outputConsumer.accept(WorkflowStreamEvent.metadata(
                "user_profile",
                "started",
                "开始准备用户画像",
                intent,
                null,
                null,
                sequence.incrementAndGet()
        ));
        String userProfile = agents.userProfileAgent().generateUserProfile(userId);
        String profileStatus = extractProfileStatus(userProfile);
        outputConsumer.accept(WorkflowStreamEvent.metadata(
                "user_profile",
                "completed",
                "用户画像准备完成",
                intent,
                profileStatus,
                null,
                sequence.incrementAndGet()
        ));
        outputConsumer.accept(WorkflowStreamEvent.metadata(
                "plan_generation",
                "streaming",
                "开始流式生成健身计划",
                intent,
                profileStatus,
                null,
                sequence.incrementAndGet()
        ));

        String result = agents.planAgent().streamPlan(userProfile, userInput, chunk -> outputConsumer.accept(
                WorkflowStreamEvent.token("plan_generation", chunk, intent, sequence.incrementAndGet())
        ));
        outputConsumer.accept(WorkflowStreamEvent.done(
                "plan_generation",
                "流式输出完成",
                intent,
                profileStatus,
                result,
                sequence.incrementAndGet()
        ));
    }

    private void streamActionGuidanceWorkflow(String userInput,
                                              Long userId,
                                              String intent,
                                              AgentBundle agents,
                                              Consumer<WorkflowStreamEvent> outputConsumer,
                                              AtomicLong sequence) {
        outputConsumer.accept(WorkflowStreamEvent.metadata(
                "action_guidance",
                "streaming",
                "开始流式生成动作指导",
                intent,
                null,
                null,
                sequence.incrementAndGet()
        ));

        String result = agents.actionAgent().streamGuidance(userInput, chunk -> outputConsumer.accept(
                WorkflowStreamEvent.token("action_guidance", chunk, intent, sequence.incrementAndGet())
        ));
        outputConsumer.accept(WorkflowStreamEvent.done(
                "action_guidance",
                "流式输出完成",
                intent,
                null,
                result,
                sequence.incrementAndGet()
        ));
    }

    private String streamChatWorkflow(String userInput,
                                    Long userId,
                                    String sessionHistory,
                                    String intent,
                                    AgentBundle agents,
                                    Consumer<WorkflowStreamEvent> outputConsumer,
                                    AtomicLong sequence) {
        outputConsumer.accept(WorkflowStreamEvent.metadata(
                "chat",
                "streaming",
                "开始流式生成回复",
                intent,
                null,
                null,
                sequence.incrementAndGet()
        ));

        String result = agents.companionAgent().streamChat(userInput, userId, sessionHistory, chunk -> outputConsumer.accept(
                WorkflowStreamEvent.token("chat", chunk, intent, sequence.incrementAndGet())
        ));
        outputConsumer.accept(WorkflowStreamEvent.done(
                "chat",
                "流式输出完成",
                intent,
                null,
                result,
                sequence.incrementAndGet()
        ));
        return result;
    }

    private String recognizeIntent(String userInput, AgentBundle agents) {
        agents.intentRouteTool().reset();
        agents.intentAgent().recognizeIntent(userInput);
        String intent = agents.intentRouteTool().getRecognizedIntent();
        if (intent == null || intent.isBlank()) {
            log.warn("未能识别意图，默认回退到 chat");
            return "chat";
        }
        return intent;
    }

    private String resolveExecutionNode(String intent) {
        return switch (intent) {
            case "plan_generation" -> "plan_generation";
            case "action_guidance" -> "action_guidance";
            default -> "chat";
        };
    }

    private String extractProfileStatus(String userProfile) {
        if (userProfile == null || userProfile.isBlank()) {
            return "DEGRADED";
        }
        if (userProfile.startsWith("DATA_STATUS: GROUNDED")) {
            return "GROUNDED";
        }
        return "DEGRADED";
    }

    private AgentBundle createAgentBundle() {
        ChatClient chatClient = chatClientBuilder.build();
        TerminateTool terminateTool = new TerminateTool();
        IntentRouteTool intentRouteTool = new IntentRouteTool();

        ToolCallback[] intentTools = MethodToolCallbackProvider.builder()
                .toolObjects(intentRouteTool, terminateTool)
                .build()
                .getToolCallbacks();

        ToolCallback[] ragAndMcpTools = combineTools(
                MethodToolCallbackProvider.builder()
                        .toolObjects(knowledgeSearchTool, terminateTool)
                        .build()
                        .getToolCallbacks(),
                mcpTools
        );

        ToolCallback[] userProfileTools = combineTools(
                MethodToolCallbackProvider.builder()
                        .toolObjects(terminateTool)
                        .build()
                        .getToolCallbacks(),
                mcpTools
        );

        // 陪伴激励 Agent 的工具集：终止 + MCP(训练记录查询) + 激励知识检索 + 情绪检测
        ToolCallback[] companionTools = combineTools(
                MethodToolCallbackProvider.builder()
                        .toolObjects(terminateTool, motivationKnowledgeSearchTool, emotionDetectionTool)
                        .build()
                        .getToolCallbacks(),
                mcpTools
        );

        return new AgentBundle(
                intentRouteTool,
                new IntentRecognitionAgent(intentTools, chatClient),
                new UserProfileAgent(userProfileTools, chatClient),
                new PlanGenerationAgent(ragAndMcpTools, chatClient),
                new ActionGuidanceAgent(ragAndMcpTools, chatClient),
                new CompanionMotivationAgent(companionTools, chatClient)
        );
    }

    private ToolCallback[] combineTools(ToolCallback[]... toolArrays) {
        int totalLength = 0;
        for (ToolCallback[] array : toolArrays) {
            totalLength += array.length;
        }

        ToolCallback[] combined = new ToolCallback[totalLength];
        int currentIndex = 0;
        for (ToolCallback[] array : toolArrays) {
            System.arraycopy(array, 0, combined, currentIndex, array.length);
            currentIndex += array.length;
        }
        return combined;
    }

    private record AgentBundle(IntentRouteTool intentRouteTool,
                               IntentRecognitionAgent intentAgent,
                               UserProfileAgent userProfileAgent,
                               PlanGenerationAgent planAgent,
                               ActionGuidanceAgent actionAgent,
                               CompanionMotivationAgent companionAgent) {
    }

    /**
     * 获取格式化的 session 对话历史
     */
    private String getSessionHistory(String sessionId) {
        if (sessionChatMemory instanceof InMemorySessionChatMemory inMemory) {
            return inMemory.formatHistoryAsText(sessionId);
        }
        return "";
    }

    /**
     * 将对话历史注入到用户输入中（用于意图识别和非陪伴 Agent）
     */
    private String buildContextualInput(String userInput, String sessionHistory) {
        if (sessionHistory == null || sessionHistory.isBlank()) {
            return userInput;
        }
        return sessionHistory + "\n【当前问题】\n" + userInput;
    }

    /**
     * 保存用户输入和 AI 回复到 session 记忆
     */
    private void saveToSessionMemory(String sessionId, String userInput, String result) {
        if (sessionId != null && result != null) {
            sessionChatMemory.add(sessionId, List.of(
                    new UserMessage(userInput),
                    new AssistantMessage(result)
            ));
        }
    }

    public record WorkflowExecutionResponse(String status,
                                            String userInput,
                                            Long userId,
                                            String sessionId,
                                            String intent,
                                            String node,
                                            String profileStatus,
                                            String result,
                                            String message) {

        public static WorkflowExecutionResponse completed(String userInput,
                                                          Long userId,
                                                          String sessionId,
                                                          String intent,
                                                          String node,
                                                          String result,
                                                          String profileStatus) {
            return new WorkflowExecutionResponse(
                    "completed",
                    userInput,
                    userId,
                    sessionId,
                    intent,
                    node,
                    profileStatus,
                    result,
                    "工作流执行完成"
            );
        }

        public static WorkflowExecutionResponse failed(String userInput, Long userId, String sessionId, String node, String message) {
            return new WorkflowExecutionResponse(
                    "failed",
                    userInput,
                    userId,
                    sessionId,
                    null,
                    node,
                    null,
                    null,
                    message
            );
        }
    }

    public record WorkflowStreamEvent(String event,
                                      String node,
                                      String status,
                                      String message,
                                      String intent,
                                      String profileStatus,
                                      String sessionId,
                                      String content,
                                      Long sequence,
                                      Boolean done) {

        public static WorkflowStreamEvent metadata(String node,
                                                   String status,
                                                   String message,
                                                   String intent,
                                                   String profileStatus,
                                                   String sessionId,
                                                   Long sequence) {
            return new WorkflowStreamEvent("metadata", node, status, message, intent, profileStatus, sessionId, null, sequence, false);
        }

        public static WorkflowStreamEvent token(String node, String content, String intent, Long sequence) {
            return new WorkflowStreamEvent("token", node, "streaming", null, intent, null, null, content, sequence, false);
        }

        public static WorkflowStreamEvent done(String node,
                                               String message,
                                               String intent,
                                               String profileStatus,
                                               String content,
                                               Long sequence) {
            return new WorkflowStreamEvent("done", node, "completed", message, intent, profileStatus, null, content, sequence, true);
        }

        public static WorkflowStreamEvent error(String node, String message, Long sequence) {
            return new WorkflowStreamEvent("error", node, "failed", message, null, null, null, null, sequence, true);
        }

        public Map<String, Object> toPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", event);
            payload.put("node", node);
            payload.put("status", status);
            payload.put("sequence", sequence);
            payload.put("done", done);
            if (message != null) {
                payload.put("message", message);
            }
            if (intent != null) {
                payload.put("intent", intent);
            }
            if (profileStatus != null) {
                payload.put("profileStatus", profileStatus);
            }
            if (sessionId != null) {
                payload.put("sessionId", sessionId);
            }
            if (content != null) {
                payload.put("content", content);
            }
            return payload;
        }
    }
}
