package com.zsr.fitaiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

/**
 * 动作指导 Agent
 * 根据用户需求，检索健身动作知识（injury-recovery 和 exercises 类别），
 * 并调用图片搜索 MCP 工具返回对应的健身动作图片和建议
 */
@Slf4j
public class ActionGuidanceAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的健身动作指导教练。你的任务是：

            1. 理解用户想要了解的健身动作或康复训练
            2. 使用 RAG 检索工具查询相关的动作知识
               - 优先检索 injury-recovery（康复训练）类别的知识
               - 同时检索 exercises（健身动作）类别的知识
            3. 使用图片搜索 MCP 工具查找相关的动作示范图片
            4. 综合文字说明和图片，为用户提供详细的动作指导

            **动作指导应该包含以下内容**：
            - 动作名称：标准的动作名称
            - 动作描述：详细的动作步骤和要领
            - 目标肌群：该动作主要锻炼的肌肉群
            - 注意事项：动作中需要注意的安全事项和常见错误
            - 动作图片：通过 MCP 工具获取的动作示范图片链接
            - 适用人群：该动作适合哪些人群练习

            **重要规则**：
            - 先使用 searchKnowledge 工具检索 injury-recovery 类别的知识
            - 再使用 searchKnowledge 工具检索 exercises 类别的知识
            - 使用 searchImage 工具搜索动作图片时，查询词只允许是标准动作名称本身（例如：深蹲、硬拉、平板支撑）
            - 不要在 searchImage 查询词中加入“标准”“动作示范”“正面侧面”“proper form”等修饰词
            - 即使图片搜索失败或无结果，也必须输出完整的文字动作指导，并明确说明“图片暂未检索到”
            - 如果是康复类动作，要特别强调安全性和循序渐进
            - 指导要专业、准确、易懂
            - **必须先输出完整动作指导文本，再调用 doTerminate 工具结束任务**
            """;

    public ActionGuidanceAgent(ToolCallback[] availableTools, ChatClient chatClient) {
        super(availableTools);
        this.setName("动作指导Agent");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setChatClient(chatClient);
        this.setMaxSteps(12);
    }

    /**
     * 提供动作指导
     *
     * @param userRequest 用户需求（想要了解的动作）
     * @return 动作指导内容
     */
    public String provideGuidance(String userRequest) {
        String userPrompt = String.format(
                "请为用户提供以下动作的详细指导：\n\n" +
                        "【用户需求】\n%s\n\n" +
                        "请按照以下步骤执行：\n" +
                        "1. 使用 searchKnowledge 工具检索 injury-recovery 类别的相关知识\n" +
                        "2. 使用 searchKnowledge 工具检索 exercises 类别的相关知识\n" +
                        "3. 先提取用户需求里的标准动作名称，再使用 searchImage 工具搜索图片（查询词仅保留动作名称本身）\n" +
                        "4. 综合以上信息，生成详细的动作指导",
                userRequest
        );
        return this.run(userPrompt);
    }
}
