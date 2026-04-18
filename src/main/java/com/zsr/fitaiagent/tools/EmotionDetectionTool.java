package com.zsr.fitaiagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 情绪检测工具
 * 基于 LLM 分析用户对话文本中的情绪状态
 * 输出结构化的情绪评估结果
 */
@Component
@Slf4j
public class EmotionDetectionTool {

    private final ChatClient chatClient;

    private static final String EMOTION_ANALYSIS_PROMPT = """
            你是一个专业的情绪分析专家。请分析以下对话文本中用户的情绪状态。

            **分析维度**：
            1. 情绪状态：积极 / 中性 / 消极 / 沮丧 / 焦虑 / 疲惫
            2. 能量水平：高 / 中 / 低
            3. 训练动力：上升 / 稳定 / 下降 / 极低
            4. 关键情绪信号：列出对话中体现情绪的关键词或表达

            **重要规则**：
            - 保守判断：如果信息不足以判断，默认为"中性"
            - 不要过度解读简单的问候或客套话
            - 关注与训练、健身、身体状态相关的情绪表达
            - 输出格式必须严格按照以下模板

            **输出格式**：
            情绪状态: [状态]
            能量水平: [水平]
            训练动力: [趋势]
            关键信号: [信号1]、[信号2]...
            建议策略: [正向强化/阶段肯定/缓冲引导/回归激励/常规陪伴]

            请分析以下对话文本：
            %s
            """;

    public EmotionDetectionTool(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Tool(description = """
            Analyze the user's emotional state from their conversation text.
            Returns a structured assessment including mood, energy level, motivation level,
            key emotional signals, and recommended motivation strategy.

            Parameters:
            - conversationText: the user's recent messages to analyze

            Returns: Structured emotion assessment
            """)
    public String detectEmotion(String conversationText) {
        log.info("开始情绪检测分析");

        try {
            String prompt = String.format(EMOTION_ANALYSIS_PROMPT, conversationText);
            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("情绪检测结果: {}", result);
            return result;

        } catch (Exception e) {
            log.error("情绪检测分析失败", e);
            // 失败时返回默认中性评估
            return """
                    情绪状态: 中性
                    能量水平: 中
                    训练动力: 稳定
                    关键信号: 无法分析
                    建议策略: 常规陪伴
                    """;
        }
    }
}
