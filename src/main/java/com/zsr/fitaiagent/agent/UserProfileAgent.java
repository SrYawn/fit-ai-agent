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
            3. 生成一份完整的用户画像报告，并明确标记是否拿到了真实用户数据

            用户画像应该包含以下维度：
            - 基本信息：用户ID、姓名、年龄、性别等
            - 健身习惯：运动频率、偏好的运动类型、运动时长等
            - 健康状况：身高、体重、BMI、健康目标等
            - 行为特征：活跃度、坚持程度、进步趋势等

            请按照以下步骤执行：
            1. 使用 getUserProfileById 工具查询用户基本信息（传入 userId）
            2. 使用 getUserTrainingRecords 工具查询用户训练记录（传入 userId）
            3. 如有必要，再使用 getUserInjuries 工具查询伤病信息（传入 userId）
            4. 基于真实查询结果生成用户画像；如果数据库查询失败、返回连接错误，或关键基础信息缺失，就进入降级模式
            5. **重要：先输出完整的用户画像报告文本，然后再调用 doTerminate 工具结束任务**

            重要提示：
            - 严禁猜测用户名，严禁凭空构造 zhangsan、lisi、wangwu 等用户名
            - 数据库工具失败时不要重试超过 1 次；如果是 JDBC 连接错误，直接降级并结束
            - 输出第一行必须是以下二选一：
              DATA_STATUS: GROUNDED
              DATA_STATUS: DEGRADED
            - 只有在拿到真实数据库记录时才能输出 DATA_STATUS: GROUNDED
            - 如果是 DATA_STATUS: DEGRADED，必须明确写出缺失原因、哪些字段缺失，以及建议补充年龄/身高/体重/伤病/训练经验
            - 生成的画像要客观、准确，不要把假设写成事实
            - **必须先输出完整报告，再调用 doTerminate，不要在调用 doTerminate 后再输出任何内容**
            """;

    public UserProfileAgent(ToolCallback[] availableTools, ChatClient chatClient) {
        super(availableTools);
        this.setName("用户画像生成Agent");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setChatClient(chatClient);
        this.setMaxSteps(8);
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
                        "必须优先使用 getUserProfileById 和 getUserTrainingRecords。" +
                        "如果真实数据不足或数据库连接失败，输出 DATA_STATUS: DEGRADED，并说明需要补充哪些信息。",
                userId
        );
        return this.run(userPrompt);
    }
}
