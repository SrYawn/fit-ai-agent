package com.zsr.fitaiagent.graph;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.zsr.fitaiagent.agent.*;
import com.zsr.fitaiagent.tools.IntentRouteTool;
import com.zsr.fitaiagent.tools.KnowledgeSearchTool;
import com.zsr.fitaiagent.tools.TerminateTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 健身工作流图
 * 使用 Spring AI Alibaba StateGraph 框架构建多 Agent 协作工作流
 *
 * 工作流程：
 * 1. 意图识别 Agent 识别用户意图
 * 2. 根据意图路由到不同的 Agent：
 *    - 计划生成：用户画像 Agent → 计划生成 Agent
 *    - 动作指导：动作指导 Agent
 *    - 闲聊：闲聊 Agent
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

    @Autowired
    private ToolCallback[] allTools;

    /**
     * 构建健身工作流图
     */
    public CompiledGraph buildWorkflow() throws Exception {
        log.info("开始构建健身工作流图...");

        ChatClient chatClient = chatClientBuilder.build();

        // 1. 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("user_input", new ReplaceStrategy());
            strategies.put("user_id", new ReplaceStrategy());
            strategies.put("intent", new ReplaceStrategy());
            strategies.put("user_profile", new ReplaceStrategy());
            strategies.put("final_result", new ReplaceStrategy());
            return strategies;
        };

        // 2. 创建工具
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

        ToolCallback[] chatTools = MethodToolCallbackProvider.builder()
                .toolObjects(terminateTool)
                .build()
                .getToolCallbacks();

        // 3. 创建业务 Agent 实例
        IntentRecognitionAgent intentAgent = new IntentRecognitionAgent(intentTools, chatClient);
        UserProfileAgent userProfileAgent = new UserProfileAgent(allTools, chatClient);
        PlanGenerationAgent planAgent = new PlanGenerationAgent(ragAndMcpTools, chatClient);
        ActionGuidanceAgent actionAgent = new ActionGuidanceAgent(ragAndMcpTools, chatClient);
        ChatAgent chatAgent = new ChatAgent(chatTools, chatClient);

        // 4. 创建工作流节点（使用已有的业务 Agent）

        // 意图识别节点
        class IntentRecognitionNode implements NodeAction {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                String userInput = state.value("user_input", "").toString();
                log.info("执行意图识别 - 用户输入: {}", userInput);

                intentAgent.recognizeIntent(userInput);
                String intent = intentRouteTool.getRecognizedIntent();

                if (intent == null) {
                    log.warn("未能识别意图，默认为闲聊");
                    intent = "chat";
                }

                log.info("识别到的意图: {}", intent);
                return Map.of("intent", intent, "_condition_result", intent);
            }
        }

        // 用户画像节点
        class UserProfileNode implements NodeAction {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                Long userId = ((Number) state.value("user_id", 1L)).longValue();
                log.info("生成用户画像 - 用户ID: {}", userId);

                String userProfile = userProfileAgent.generateUserProfile(userId);
                log.info("用户画像生成完成");

                return Map.of("user_profile", userProfile);
            }
        }

        // 计划生成节点
        class PlanGenerationNode implements NodeAction {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                String userProfile = state.value("user_profile", "").toString();
                String userInput = state.value("user_input", "").toString();
                log.info("生成健身计划");

                String plan = planAgent.generatePlan(userProfile, userInput);
                log.info("健身计划生成完成");

                return Map.of("final_result", plan);
            }
        }

        // 动作指导节点
        class ActionGuidanceNode implements NodeAction {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                String userInput = state.value("user_input", "").toString();
                log.info("提供动作指导");

                String guidance = actionAgent.provideGuidance(userInput);
                log.info("动作指导完成");

                return Map.of("final_result", guidance);
            }
        }

        // 闲聊节点
        class ChatNode implements NodeAction {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                String userInput = state.value("user_input", "").toString();
                log.info("处理闲聊");

                String response = chatAgent.chat(userInput);
                log.info("闲聊处理完成");

                return Map.of("final_result", response);
            }
        }

        // 5. 构建 StateGraph
        StateGraph workflow = new StateGraph(keyStrategyFactory);

        // 添加节点
        workflow.addNode("intent_recognition", node_async(new IntentRecognitionNode()));
        workflow.addNode("user_profile", node_async(new UserProfileNode()));
        workflow.addNode("plan_generation", node_async(new PlanGenerationNode()));
        workflow.addNode("action_guidance", node_async(new ActionGuidanceNode()));
        workflow.addNode("chat", node_async(new ChatNode()));

        // 6. 定义工作流程
        workflow.addEdge(StateGraph.START, "intent_recognition");

        // 条件路由
        workflow.addConditionalEdges(
                "intent_recognition",
                edge_async(state -> state.value("_condition_result", "chat").toString()),
                Map.of(
                        "plan_generation", "user_profile",
                        "action_guidance", "action_guidance",
                        "chat", "chat"
                )
        );

        // 计划生成流程：用户画像 -> 计划生成
        workflow.addEdge("user_profile", "plan_generation");

        // 设置结束点
        workflow.addEdge("plan_generation", StateGraph.END);
        workflow.addEdge("action_guidance", StateGraph.END);
        workflow.addEdge("chat", StateGraph.END);

        // 7. 编译图
        CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
        log.info("健身工作流图构建完成");

        return compiledGraph;
    }

    /**
     * 执行工作流
     */
    public String executeWorkflow(String userInput, Long userId) {
        log.info("执行工作流 - 用户输入: {}, 用户ID: {}", userInput, userId);

        try {
            CompiledGraph graph = buildWorkflow();

            Map<String, Object> input = Map.of(
                    "user_input", userInput,
                    "user_id", userId != null ? userId : 1L
            );

            // 同步执行
            NodeOutput result = graph.stream(input).blockLast();

            if (result != null) {
                Object finalResult = result.state().value("final_result").orElse("处理完成");
                return finalResult.toString();
            }

            return "执行失败";
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            return "执行失败：" + e.getMessage();
        }
    }

    /**
     * 流式执行工作流
     */
    public void executeWorkflowStream(String userInput, Long userId,
                                      java.util.function.Consumer<String> outputConsumer) {
        log.info("流式执行工作流 - 用户输入: {}, 用户ID: {}", userInput, userId);

        try {
            CompiledGraph graph = buildWorkflow();

            Map<String, Object> input = Map.of(
                    "user_input", userInput,
                    "user_id", userId != null ? userId : 1L
            );

            outputConsumer.accept("工作流开始执行...");
            AtomicBoolean finalResultSent = new AtomicBoolean(false);
            AtomicReference<String> intentSent = new AtomicReference<>(null);

            // 当前工作流主要输出 NodeOutput，而非 StreamingOutput，因此需要从状态中提取结果
            NodeOutput lastOutput = graph.stream(input).doOnNext(output -> {
                if (output instanceof NodeOutput nodeOutput) {
                    nodeOutput.state().value("intent")
                            .map(Object::toString)
                            .ifPresent(intent -> {
                                if (intentSent.compareAndSet(null, intent)) {
                                    outputConsumer.accept("识别意图：" + intent);
                                }
                            });

                    nodeOutput.state().value("final_result")
                            .map(Object::toString)
                            .ifPresent(finalResult -> {
                                if (finalResultSent.compareAndSet(false, true)) {
                                    outputConsumer.accept(finalResult);
                                }
                            });
                }
            }).blockLast();

            if (!finalResultSent.get() && lastOutput != null) {
                Object finalResult = lastOutput.state().value("final_result").orElse(null);
                if (finalResult != null) {
                    outputConsumer.accept(finalResult.toString());
                    finalResultSent.set(true);
                }
            }

            if (!finalResultSent.get()) {
                outputConsumer.accept("处理完成，但未生成可输出的结果");
            }
        } catch (Exception e) {
            log.error("流式执行工作流失败", e);
            outputConsumer.accept("执行失败：" + e.getMessage());
        }
    }

    /**
     * 合并工具数组
     */
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
}
