package com.zsr.fitaiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

/**
 * 用户画像生成 Agent
 * 通过调用 MCP 工具查询 MySQL 数据库，构建用户画像
 */
@Slf4j
public class UserProfileAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的用户画像分析专家。你的任务是：

            1. 使用可用的数据库查询工具，查询用户的相关信息
            2. 分析用户的健身数据、行为习惯、偏好等
            3. 生成一份完整的用户画像报告

            用户画像应该包含以下维度：
            - 基本信息：用户ID、姓名、年龄、性别等
            - 健身习惯：运动频率、偏好的运动类型、运动时长等
            - 健康状况：身高、体重、BMI、健康目标等
            - 行为特征：活跃度、坚持程度、进步趋势等

            请按照以下步骤执行：
            1. 使用 getUserTrainingRecords 工具查询用户的健身记录（传入用户ID，例如：1）
            2. 分析训练记录数据，了解用户的运动习惯
            3. 尝试使用 getUserProfile 工具查询用户基本信息（需要传入 username，例如："zhangsan"）
               - 如果不知道 username，可以尝试常见的用户名，如 "zhangsan", "lisi", "wangwu" 等
               - 如果查询失败，不要反复尝试超过3次，直接使用训练记录数据即可
            4. 基于收集到的所有数据，生成详细的用户画像分析报告
            5. **重要：先输出完整的用户画像报告文本，然后再调用 doTerminate 工具结束任务**

            重要提示：
            - getUserTrainingRecords 接受 userId (Long类型，例如：1)
            - getUserProfile 接受 username (String类型，例如："zhangsan")
            - 不要浪费步骤反复尝试查询，最多尝试3次 getUserProfile
            - 即使没有用户基本信息，也可以基于训练记录生成有价值的画像
            - 生成的画像要客观、准确、有洞察力
            - **必须先输出完整报告，再调用 doTerminate，不要在调用 doTerminate 后再输出任何内容**
            """;

    public UserProfileAgent(ToolCallback[] availableTools, ChatClient chatClient) {
        super(availableTools);
        this.setName("用户画像生成Agent");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setChatClient(chatClient);
        this.setMaxSteps(15); // 增加最大步骤数，确保有足够的步骤完成任务
    }

    /**
     * 生成用户画像
     *
     * @param userId 用户ID
     * @return 用户画像报告
     */
    public String generateUserProfile(Long userId) {
        String userPrompt = String.format(
                "请为用户ID=%d生成完整的用户画像。" +
                        "首先查询用户基本信息，然后查询健身记录，最后生成画像报告。",
                userId
        );
        return this.run(userPrompt);
    }
}
