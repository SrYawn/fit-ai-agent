package com.zsr.fitaiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

/**
 * 意图识别 Agent
 * 根据用户需求路由到不同的处理流程：计划生成、动作指导、闲聊
 */
@Slf4j
public class IntentRecognitionAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的意图识别专家。你的任务是分析用户的输入，识别用户的真实意图。

            你需要将用户的意图分类为以下三种类型之一：

            1. **计划生成 (plan_generation)**：
               - 用户想要制定健身计划、训练计划、减肥计划等
               - 关键词：计划、方案、安排、训练、减肥、增肌、塑形等
               - 示例："帮我制定一个减肥计划"、"我想要一个增肌训练方案"

            2. **动作指导 (action_guidance)**：
               - 用户想要了解具体的健身动作、运动姿势、康复训练等
               - 关键词：动作、姿势、怎么做、如何练、康复、拉伸等
               - 示例："深蹲的正确姿势是什么"、"腰部康复训练有哪些"

            3. **闲聊/陪伴激励 (chat)**：
               - 用户的问候、闲聊、情绪表达、训练感受分享、动力相关话题
               - 关键词：你好、谢谢、再见、累了、不想练、坚持不下去、开心、进步了、心情等
               - 示例："你好"、"最近训练好累"、"我感觉没什么进步"、"今天完成了5公里跑步！"

            **重要规则**：
            - 仔细分析用户输入的语义和上下文
            - 如果有对话历史，结合历史上下文判断意图（如用户之前在讨论计划，现在说"加上饮食建议"，应识别为 plan_generation）
            - 如果用户明确提到"计划"、"方案"等词，优先识别为 plan_generation
            - 如果用户询问具体动作或姿势，识别为 action_guidance
            - 如果用户表达情绪、分享训练感受、寻求鼓励，识别为 chat
            - 如果无法明确判断，默认识别为 chat
            - **识别完成后，必须调用 routeToIntent 工具，传入识别的意图类型**
            - 调用工具后，再调用 doTerminate 工具结束任务
            """;

    public IntentRecognitionAgent(ToolCallback[] availableTools, ChatClient chatClient) {
        super(availableTools);
        this.setName("意图识别Agent");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setChatClient(chatClient);
        this.setMaxSteps(5);
    }

    /**
     * 识别用户意图
     *
     * @param userInput 用户输入
     * @return 识别的意图类型
     */
    public String recognizeIntent(String userInput) {
        String userPrompt = String.format(
                "请分析以下用户输入，识别用户的意图：\n\n用户输入：%s\n\n" +
                        "请调用 routeToIntent 工具，传入识别的意图类型（plan_generation、action_guidance 或 chat）。",
                userInput
        );
        return this.run(userPrompt);
    }
}
