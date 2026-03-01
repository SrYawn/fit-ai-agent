package com.zsr.fitaiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

/**
 * 计划生成 Agent
 * 根据用户画像 + 用户需求 + RAG 检索的健身知识，生成个性化健身计划
 */
@Slf4j
public class PlanGenerationAgent extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的健身计划制定专家。你的任务是：

            1. 接收用户画像信息（包含用户的基本信息、健身习惯、健康状况等）
            2. 理解用户的健身需求和目标
            3. 使用 RAG 检索工具查询相关的健身知识（注意：只使用用户的计划需求作为查询关键词，不要加上用户画像信息）
            4. 综合以上信息，生成一份科学、个性化的健身计划

            **健身计划应该包含以下内容**：
            - 训练目标：明确用户的健身目标（减脂、增肌、塑形、康复等）
            - 训练周期：建议的训练周期（如 4 周、8 周、12 周等）
            - 训练频率：每周训练几次，每次多长时间
            - 训练内容：具体的训练动作、组数、次数、强度等
            - 饮食建议：配合训练的饮食建议
            - 注意事项：根据用户的健康状况给出注意事项

            **重要规则**：
            - 使用 searchKnowledge 工具检索健身知识时，query 参数只传入用户的计划需求关键词
            - 不要在 query 中包含用户画像的详细信息，保持查询简洁
            - 例如：用户需求是"减肥计划"，query 就传 "减肥训练方法"
            - 建议至少检索 2 个维度：training-plan（训练安排）和 nutrition（饮食建议）
            - 计划要科学合理，符合用户的实际情况
            - 计划要具体可执行，不要过于笼统
            - **必须先输出完整健身计划文本，再调用 doTerminate 工具结束任务**
            - **不要在只返回工具结果后立即结束任务**
            """;

    public PlanGenerationAgent(ToolCallback[] availableTools, ChatClient chatClient) {
        super(availableTools);
        this.setName("计划生成Agent");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setChatClient(chatClient);
        this.setMaxSteps(10);
    }

    /**
     * 生成健身计划
     *
     * @param userProfile 用户画像
     * @param userRequest 用户需求
     * @return 健身计划
     */
    public String generatePlan(String userProfile, String userRequest) {
        String userPrompt = String.format(
                "请根据以下信息生成个性化健身计划：\n\n" +
                        "【用户画像】\n%s\n\n" +
                        "【用户需求】\n%s\n\n" +
                        "请按以下步骤执行：\n" +
                        "1. 使用 searchKnowledge 检索 training-plan 类别知识（query 仅使用用户需求关键词）\n" +
                        "2. 使用 searchKnowledge 检索 nutrition 类别知识（query 仅使用用户需求关键词）\n" +
                        "3. 综合用户画像与检索结果，先输出完整的一周计划（包含训练与饮食安排）\n" +
                        "4. 输出完成后再调用 doTerminate 结束任务",
                userProfile, userRequest
        );
        return this.run(userPrompt);
    }
}
