package com.zsr.fitaiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.function.Consumer;

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
            - 如果用户画像状态是 DATA_STATUS: GROUNDED，才允许生成“个性化计划”，并且只能引用画像里真实存在的数据
            - 如果用户画像状态是 DATA_STATUS: DEGRADED，必须显式说明当前只能提供“通用起步计划”，不能伪装成个性化计划
            - 降级时必须先列出需要用户补充的关键字段：年龄、身高、体重、伤病情况、训练经验、训练目标
            - 无论是否降级，计划都要科学合理、具体可执行，不要过于笼统
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
                "请根据以下信息生成健身计划：\n\n" +
                        "【用户画像】\n%s\n\n" +
                        "【用户需求】\n%s\n\n" +
                        "请按以下步骤执行：\n" +
                        "1. 使用 searchKnowledge 检索 training-plan 类别知识（query 仅使用用户需求关键词）\n" +
                        "2. 使用 searchKnowledge 检索 nutrition 类别知识（query 仅使用用户需求关键词）\n" +
                        "3. 先判断用户画像是 DATA_STATUS: GROUNDED 还是 DATA_STATUS: DEGRADED\n" +
                        "4. 如果是 GROUNDED，输出基于真实画像的一周个性化计划（包含训练与饮食安排），不要编造画像细节\n" +
                        "5. 如果是 DEGRADED，明确写明“当前为通用起步计划”，先列需要补充的信息，再给出保守、安全的一周通用计划\n" +
                        "6. 输出完成后再调用 doTerminate 结束任务",
                userProfile, userRequest
        );
        return this.run(userPrompt);
    }

    public String streamPlan(String userProfile, String userRequest, Consumer<String> tokenConsumer) {
        String userPrompt = String.format(
                "请根据以下信息生成健身计划：\n\n" +
                        "【用户画像】\n%s\n\n" +
                        "【用户需求】\n%s\n\n" +
                        "请按以下步骤执行：\n" +
                        "1. 使用 searchKnowledge 检索 training-plan 类别知识（query 仅使用用户需求关键词）\n" +
                        "2. 使用 searchKnowledge 检索 nutrition 类别知识（query 仅使用用户需求关键词）\n" +
                        "3. 先判断用户画像是 DATA_STATUS: GROUNDED 还是 DATA_STATUS: DEGRADED\n" +
                        "4. 如果是 GROUNDED，输出基于真实画像的一周个性化计划（包含训练与饮食安排），不要编造画像细节\n" +
                        "5. 如果是 DEGRADED，明确写明“当前为通用起步计划”，先列需要补充的信息，再给出保守、安全的一周通用计划\n" +
                        "6. 输出完成后再调用 doTerminate 结束任务",
                userProfile, userRequest
        );
        return this.runWithStreamingFinalAnswer(userPrompt, tokenConsumer);
    }
}
