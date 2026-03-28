package com.zsr.fitaiagent.graph;

import com.zsr.fitaiagent.agent.ActionGuidanceAgent;
import com.zsr.fitaiagent.agent.ChatAgent;
import com.zsr.fitaiagent.agent.IntentRecognitionAgent;
import com.zsr.fitaiagent.agent.PlanGenerationAgent;
import com.zsr.fitaiagent.agent.UserProfileAgent;
import com.zsr.fitaiagent.tools.IntentRouteTool;
import com.zsr.fitaiagent.tools.KnowledgeSearchTool;
import com.zsr.fitaiagent.tools.TerminateTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
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
    private ToolCallback[] mcpTools;

    public WorkflowExecutionResponse executeWorkflow(String userInput, Long userId) {
        Long resolvedUserId = userId != null ? userId : 1L;
        log.info("执行工作流 - 用户输入: {}, 用户ID: {}", userInput, resolvedUserId);

        try {
            AgentBundle agents = createAgentBundle();
            String intent = recognizeIntent(userInput, agents);

            return switch (intent) {
                case "plan_generation" -> executePlanWorkflow(userInput, resolvedUserId, intent, agents);
                case "action_guidance" -> executeActionGuidanceWorkflow(userInput, resolvedUserId, intent, agents);
                default -> executeChatWorkflow(userInput, resolvedUserId, intent, agents);
            };
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            return WorkflowExecutionResponse.failed(userInput, resolvedUserId, "workflow", "执行失败：" + e.getMessage());
        }
    }

    public void executeWorkflowStream(String userInput, Long userId, Consumer<WorkflowStreamEvent> outputConsumer) {
        Long resolvedUserId = userId != null ? userId : 1L;
        AtomicLong sequence = new AtomicLong(0L);
        log.info("流式执行工作流 - 用户输入: {}, 用户ID: {}", userInput, resolvedUserId);

        try {
            AgentBundle agents = createAgentBundle();
            outputConsumer.accept(WorkflowStreamEvent.metadata(
                    "workflow",
                    "started",
                    "工作流开始执行",
                    null,
                    null,
                    sequence.incrementAndGet()
            ));

            String intent = recognizeIntent(userInput, agents);
            String executionNode = resolveExecutionNode(intent);
            outputConsumer.accept(WorkflowStreamEvent.metadata(
                    executionNode,
                    "routed",
                    "识别意图：" + intent,
                    intent,
                    null,
                    sequence.incrementAndGet()
            ));

            switch (intent) {
                case "plan_generation" -> streamPlanWorkflow(
                        userInput,
                        resolvedUserId,
                        intent,
                        agents,
                        outputConsumer,
                        sequence
                );
                case "action_guidance" -> streamActionGuidanceWorkflow(
                        userInput,
                        resolvedUserId,
                        intent,
                        agents,
                        outputConsumer,
                        sequence
                );
                default -> streamChatWorkflow(
                        userInput,
                        resolvedUserId,
                        intent,
                        agents,
                        outputConsumer,
                        sequence
                );
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

    private WorkflowExecutionResponse executePlanWorkflow(String userInput, Long userId, String intent, AgentBundle agents) {
        String userProfile = agents.userProfileAgent().generateUserProfile(userId);
        String profileStatus = extractProfileStatus(userProfile);
        String result = agents.planAgent().generatePlan(userProfile, userInput);
        return WorkflowExecutionResponse.completed(userInput, userId, intent, "plan_generation", result, profileStatus);
    }

    private WorkflowExecutionResponse executeActionGuidanceWorkflow(String userInput, Long userId, String intent, AgentBundle agents) {
        String result = agents.actionAgent().provideGuidance(userInput);
        return WorkflowExecutionResponse.completed(userInput, userId, intent, "action_guidance", result, null);
    }

    private WorkflowExecutionResponse executeChatWorkflow(String userInput, Long userId, String intent, AgentBundle agents) {
        String result = agents.chatAgent().chat(userInput);
        return WorkflowExecutionResponse.completed(userInput, userId, intent, "chat", result, null);
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
                sequence.incrementAndGet()
        ));
        outputConsumer.accept(WorkflowStreamEvent.metadata(
                "plan_generation",
                "streaming",
                "开始流式生成健身计划",
                intent,
                profileStatus,
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

    private void streamChatWorkflow(String userInput,
                                    Long userId,
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
                sequence.incrementAndGet()
        ));

        String result = agents.chatAgent().streamChat(userInput, chunk -> outputConsumer.accept(
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

        ToolCallback[] chatTools = MethodToolCallbackProvider.builder()
                .toolObjects(terminateTool)
                .build()
                .getToolCallbacks();

        ToolCallback[] ragAndMcpTools = combineTools(
                MethodToolCallbackProvider.builder()
                        .toolObjects(knowledgeSearchTool, terminateTool)
                        .build()
                        .getToolCallbacks(),
                mcpTools
        );

        ToolCallback[] userProfileTools = combineTools(chatTools, mcpTools);

        return new AgentBundle(
                intentRouteTool,
                new IntentRecognitionAgent(intentTools, chatClient),
                new UserProfileAgent(userProfileTools, chatClient),
                new PlanGenerationAgent(ragAndMcpTools, chatClient),
                new ActionGuidanceAgent(ragAndMcpTools, chatClient),
                new ChatAgent(chatTools, chatClient)
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
                               ChatAgent chatAgent) {
    }

    public record WorkflowExecutionResponse(String status,
                                            String userInput,
                                            Long userId,
                                            String intent,
                                            String node,
                                            String profileStatus,
                                            String result,
                                            String message) {

        public static WorkflowExecutionResponse completed(String userInput,
                                                          Long userId,
                                                          String intent,
                                                          String node,
                                                          String result,
                                                          String profileStatus) {
            return new WorkflowExecutionResponse(
                    "completed",
                    userInput,
                    userId,
                    intent,
                    node,
                    profileStatus,
                    result,
                    "工作流执行完成"
            );
        }

        public static WorkflowExecutionResponse failed(String userInput, Long userId, String node, String message) {
            return new WorkflowExecutionResponse(
                    "failed",
                    userInput,
                    userId,
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
                                      String content,
                                      Long sequence,
                                      Boolean done) {

        public static WorkflowStreamEvent metadata(String node,
                                                   String status,
                                                   String message,
                                                   String intent,
                                                   String profileStatus,
                                                   Long sequence) {
            return new WorkflowStreamEvent("metadata", node, status, message, intent, profileStatus, null, sequence, false);
        }

        public static WorkflowStreamEvent token(String node, String content, String intent, Long sequence) {
            return new WorkflowStreamEvent("token", node, "streaming", null, intent, null, content, sequence, false);
        }

        public static WorkflowStreamEvent done(String node,
                                               String message,
                                               String intent,
                                               String profileStatus,
                                               String content,
                                               Long sequence) {
            return new WorkflowStreamEvent("done", node, "completed", message, intent, profileStatus, content, sequence, true);
        }

        public static WorkflowStreamEvent error(String node, String message, Long sequence) {
            return new WorkflowStreamEvent("error", node, "failed", message, null, null, null, sequence, true);
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
            if (content != null) {
                payload.put("content", content);
            }
            return payload;
        }
    }
}
