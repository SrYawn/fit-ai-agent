package com.zsr.fitaiagent.tools;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 意图路由工具
 * 用于意图识别 Agent 标记识别出的意图类型
 */
@Slf4j
public class IntentRouteTool {

    @Getter
    private String recognizedIntent = null;

    @Tool(description = """
            Route to a specific intent based on user input analysis.
            This tool should be called after analyzing the user's intent.

            Parameters:
            - intent: The recognized intent type. Must be one of:
              * "plan_generation" - User wants to create a fitness plan
              * "action_guidance" - User wants to learn about specific exercises
              * "chat" - User is having casual conversation

            Returns: Confirmation message
            """)
    public String routeToIntent(String intent) {
        log.info("路由到意图: {}", intent);

        // 验证意图类型
        if (!intent.equals("plan_generation") &&
                !intent.equals("action_guidance") &&
                !intent.equals("chat")) {
            return "错误：无效的意图类型。必须是 plan_generation、action_guidance 或 chat 之一。";
        }

        this.recognizedIntent = intent;
        return String.format("已识别用户意图为：%s", intent);
    }

    /**
     * 重置意图
     */
    public void reset() {
        this.recognizedIntent = null;
    }
}
