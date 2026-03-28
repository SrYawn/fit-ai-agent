package com.zsr.fitaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zsr.fitaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            log.info("检测到 doTerminate 调用，任务结束");
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    /**
     * 在完成工具调用准备后，以真实增量文本流输出最终答复。
     *
     * @param userPrompt    用户提示词
     * @param tokenConsumer token 消费者
     * @return 聚合后的最终文本
     */
    public String runWithStreamingFinalAnswer(String userPrompt, Consumer<String> tokenConsumer) {
        if (this.getState() != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.getState());
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }

        this.setState(AgentState.RUNNING);
        this.getMessageList().add(new UserMessage(userPrompt));

        try {
            for (int i = 0; i < this.getMaxSteps() && this.getState() != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                this.setCurrentStep(stepNumber);
                log.info("Executing streaming step {}/{}", stepNumber, this.getMaxSteps());

                AssistantMessage assistantMessage = thinkForStreaming();
                List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

                if (toolCallList.isEmpty()) {
                    return streamFinalAnswer(tokenConsumer);
                }

                act();
            }

            if (this.getCurrentStep() >= this.getMaxSteps()) {
                log.warn("{} reached max steps during streaming execution", this.getName());
            }

            return streamFinalAnswer(tokenConsumer);
        } catch (Exception e) {
            this.setState(AgentState.ERROR);
            log.error("error executing agent with streaming final answer", e);
            throw new RuntimeException("执行错误：" + e.getMessage(), e);
        } finally {
            this.cleanup();
        }
    }

    private AssistantMessage thinkForStreaming() {
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }

        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ChatResponse chatResponse = getChatClient().prompt(prompt)
                .system(getSystemPrompt())
                .toolCallbacks(availableTools)
                .call()
                .chatResponse();

        this.toolCallChatResponse = chatResponse;
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
        log.info("{}的流式预处理思考：{}", getName(), assistantMessage.getText());
        log.info("{}选择了 {} 个工具来使用", getName(), toolCallList.size());
        if (!toolCallList.isEmpty()) {
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
        }
        return assistantMessage;
    }

    private String streamFinalAnswer(Consumer<String> tokenConsumer) {
        this.getMessageList().add(new UserMessage("""
                请基于当前上下文和已有工具结果，直接输出最终给用户的答复。
                不要调用任何工具，包括 doTerminate。
                不要输出你的思考过程，只输出最终内容。
                """));

        StringBuilder contentBuilder = new StringBuilder();
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        getChatClient().prompt(prompt)
                .system(getSystemPrompt())
                .stream()
                .content()
                .doOnNext(chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        contentBuilder.append(chunk);
                        tokenConsumer.accept(chunk);
                    }
                })
                .blockLast();

        String finalAnswer = contentBuilder.toString();
        if (StrUtil.isNotBlank(finalAnswer)) {
            getMessageList().add(new AssistantMessage(finalAnswer));
        }
        this.setState(AgentState.FINISHED);
        return finalAnswer;
    }
}
