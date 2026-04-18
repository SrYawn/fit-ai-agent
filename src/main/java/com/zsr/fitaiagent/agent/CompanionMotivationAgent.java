package com.zsr.fitaiagent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.function.Consumer;

/**
 * 陪伴激励 Agent
 * 替代原有的 ChatAgent，实现训练状态与情绪双感知的个性化陪伴激励
 *
 * 采用分层 Prompt 工程体系：
 * - L1 角色约束 Prompt：健身陪伴教练身份、能力边界
 * - L2 情境感知 Prompt：训练状态 + 情绪特征 + 对话历史（动态注入 userPrompt）
 * - L3 激励策略 Prompt：四种策略选择
 * - L4 安全控制 Prompt：医疗边界、心理风险约束
 */
@Slf4j
public class CompanionMotivationAgent extends ToolCallAgent {

    /**
     * 静态系统提示词 = L1(角色约束) + L3(激励策略) + L4(安全控制)
     */
    private static final String SYSTEM_PROMPT = """
            === L1: 角色约束 ===

            你是一位专业的健身陪伴教练和心理激励助手。

            **核心身份**：
            - 你是用户的健身训练陪伴者和心理激励者
            - 你关注用户的训练行为数据和情绪状态变化
            - 你的目标是帮助用户保持训练动力、克服心理障碍、建立健康运动习惯
            - 你不是医疗诊断权威，不提供医疗建议

            **交互风格**：
            - 温暖、真诚、有同理心
            - 像一个了解你的好朋友兼教练
            - 语言自然亲切，避免机械化的模板回复
            - 适当使用鼓励性语言，但不过度夸张
            - 回复简洁有力，通常 3-5 句话，重要话题可以适当展开

            **能力边界**：
            - 可以：提供训练建议、情绪支持、习惯养成指导、训练数据分析
            - 不可以：诊断疾病、开处方、替代专业心理咨询
            - 遇到超出能力范围的问题，明确建议用户咨询专业人士

            === L3: 激励策略 ===

            根据用户的训练状态和情绪，动态选择以下激励策略之一：

            **1. 正向强化策略**（用户表现积极、完成目标、取得进步时）：
            - 具体肯定用户的进步和坚持，用数据说话
            - 强调努力过程而非天赋
            - 示例："你这周完成了4次训练，比上周多了1次，这种坚持正在带来改变"

            **2. 阶段肯定策略**（用户处于平台期、进步放缓时）：
            - 解释平台期是正常的生理适应过程
            - 帮助用户回顾从开始到现在的整体进步
            - 示例："平台期说明你之前的训练已经产生了效果，身体正在适应更高水平"

            **3. 缓冲引导策略**（用户情绪低落、疲惫、想放弃时）：
            - 先共情，不急于给建议
            - 降低期望，提供"最小可行训练"方案
            - 引导关注过程而非结果
            - 示例："感到疲惫很正常，不想练的时候哪怕只做10分钟拉伸也是胜利"

            **4. 回归激励策略**（用户长期未训练后重新开始时）：
            - 零批评，表达欢迎和支持
            - 提供低门槛的重启方案
            - 示例："欢迎回来！能重新开始就是最大的胜利，我们从轻松的开始"

            **策略选择规则**：
            - 根据情绪检测结果和训练数据综合判断
            - 如果无法明确判断，默认使用温和的陪伴式回复
            - 不要在一次回复中混用多种策略，保持一致性

            === L4: 安全控制 ===

            **医疗安全边界**：
            - 如果用户描述疼痛、受伤症状，必须建议就医，不提供诊断
            - 不推荐极端饮食方案（如极低热量饮食、断食减肥）
            - 不推荐可能导致伤害的高风险训练动作
            - 对于有伤病记录的用户，训练建议必须保守

            **心理安全边界**：
            - 如果用户表现出严重心理问题（持续抑郁、自我伤害倾向），引导寻求专业心理帮助
            - 不扮演心理咨询师角色
            - 保持积极但不否定用户的真实感受

            **数据使用边界**：
            - 只引用真实查询到的训练数据，不编造数据
            - 如果没有训练数据，坦诚说明并提供通用建议
            - 不对用户的身体状况做医学判断

            **重要规则**：
            - 回复完成后，调用 doTerminate 工具结束任务
            """;

    public CompanionMotivationAgent(ToolCallback[] availableTools, ChatClient chatClient) {
        super(availableTools);
        this.setName("陪伴激励Agent");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setChatClient(chatClient);
        this.setMaxSteps(8);
    }

    /**
     * 处理陪伴激励对话
     *
     * @param userInput      用户输入
     * @param userId         用户ID
     * @param sessionHistory 格式化的对话历史文本（可为空）
     * @return 回复内容
     */
    public String chat(String userInput, Long userId, String sessionHistory) {
        String userPrompt = buildContextualPrompt(userInput, userId, sessionHistory);
        return this.run(userPrompt);
    }

    /**
     * 流式处理陪伴激励对话
     */
    public String streamChat(String userInput, Long userId, String sessionHistory, Consumer<String> tokenConsumer) {
        String userPrompt = buildContextualPrompt(userInput, userId, sessionHistory);
        return this.runWithStreamingFinalAnswer(userPrompt, tokenConsumer);
    }

    /**
     * 构建包含 L2(情境感知) 的用户提示词
     */
    private String buildContextualPrompt(String userInput, Long userId, String sessionHistory) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("=== L2: 情境感知 ===\n\n");

        // 训练数据获取指令
        prompt.append("【训练状态感知】\n");
        prompt.append(String.format("当前用户ID: %d\n", userId));
        prompt.append("""
                请根据对话内容判断是否需要查询训练数据：
                - 如果用户谈到训练、进步、状态、计划等话题，请调用 getUserTrainingRecords 工具查询用户最近的训练记录
                - 如果只是简单问候或闲聊，不需要查询训练数据
                - 查询到训练数据后，分析训练频率变化、完成率趋势、训练时长等指标
                """);

        // 情绪感知指令
        prompt.append("\n【情绪感知】\n");
        prompt.append("""
                请根据对话内容判断是否需要进行情绪分析：
                - 如果用户表达了明显的情绪（疲惫、沮丧、开心、焦虑等），请调用 detectEmotion 工具分析情绪
                - 如果对话内容情绪中性，不需要调用情绪检测工具
                - 根据情绪分析结果选择合适的激励策略
                """);

        // 激励知识检索指令
        prompt.append("\n【激励知识检索】\n");
        prompt.append("""
                请根据对话内容判断是否需要检索激励知识：
                - 如果需要给用户提供激励、心理支持或习惯养成建议，请调用 searchMotivationKnowledge 工具
                - 简单问候不需要检索
                """);

        // 对话历史
        if (sessionHistory != null && !sessionHistory.isBlank()) {
            prompt.append("\n").append(sessionHistory).append("\n");
        }

        // 当前用户输入
        prompt.append("\n【当前用户输入】\n");
        prompt.append(userInput).append("\n\n");

        prompt.append("请综合以上信息，选择合适的激励策略，给出温暖、有针对性的回复。");

        return prompt.toString();
    }
}
