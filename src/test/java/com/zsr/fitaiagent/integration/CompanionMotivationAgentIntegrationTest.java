package com.zsr.fitaiagent.integration;

import com.zsr.fitaiagent.graph.FitnessWorkflowGraph;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 陪伴激励助手功能集成测试
 * 验证 CompanionMotivationAgent 的情绪感知、训练数据感知和激励策略
 */
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class CompanionMotivationAgentIntegrationTest {

    @Autowired
    private FitnessWorkflowGraph fitnessWorkflowGraph;

    @Test
    @DisplayName("测试1: 简单问候 - 应路由到陪伴激励 Agent")
    void testSimpleGreeting() {
        String userInput = "你好";
        Long userId = 1L;
        String sessionId = "test-greeting-" + System.currentTimeMillis();

        var response = fitnessWorkflowGraph.executeWorkflow(userInput, userId, sessionId);

        assertEquals("completed", response.status());
        assertEquals("chat", response.intent(), "问候应识别为 chat");
        assertNotNull(response.result());
        assertTrue(response.result().length() > 0, "应有回复内容");

        log.info("✓ 测试1通过");
        log.info("用户输入: {}", userInput);
        log.info("AI 回复: {}", response.result());
    }

    @Test
    @DisplayName("测试2: 情绪表达 - 疲惫/沮丧场景")
    void testEmotionExpression_Fatigue() {
        String userInput = "最近训练好累，感觉坚持不下去了";
        Long userId = 1L;
        String sessionId = "test-fatigue-" + System.currentTimeMillis();

        var response = fitnessWorkflowGraph.executeWorkflow(userInput, userId, sessionId);

        log.info("测试2 - status: {}, intent: {}, result: {}", response.status(), response.intent(), response.result());
        assertEquals("completed", response.status());
        assertEquals("chat", response.intent(), "情绪表达应识别为 chat");
        assertNotNull(response.result());

        // 验证回复包含共情或激励内容
        String result = response.result().toLowerCase();
        boolean hasEmpathy = result.contains("理解") || result.contains("正常") || result.contains("休息")
                || result.contains("疲惫") || result.contains("累");
        assertTrue(hasEmpathy, "回复应包含共情或理解的内容");

        log.info("✓ 测试2通过 - 疲惫场景");
        log.info("用户输入: {}", userInput);
        log.info("AI 回复: {}", response.result());
    }

    @Test
    @DisplayName("测试3: 积极反馈 - 正向强化场景")
    void testPositiveFeedback() {
        String userInput = "今天完成了5公里跑步，感觉很棒！";
        Long userId = 1L;
        String sessionId = "test-positive-" + System.currentTimeMillis();

        var response = fitnessWorkflowGraph.executeWorkflow(userInput, userId, sessionId);

        log.info("测试3 - status: {}, intent: {}, result: {}", response.status(), response.intent(), response.result());
        assertEquals("completed", response.status());
        assertEquals("chat", response.intent());
        assertNotNull(response.result());
        String result = response.result().toLowerCase();
        boolean hasEncouragement = result.contains("棒") || result.contains("好") || result.contains("继续")
                || result.contains("坚持") || result.contains("进步") || result.contains("不错");
        assertTrue(hasEncouragement, "回复应包含肯定或鼓励的内容");

        log.info("✓ 测试3通过 - 正向强化场景");
        log.info("用户输入: {}", userInput);
        log.info("AI 回复: {}", response.result());
    }

    @Test
    @DisplayName("测试4: 训练状态询问 - 应调用 MCP 工具查询训练数据")
    void testTrainingStateInquiry() {
        String userInput = "我最近练得怎么样？";
        Long userId = 1L;
        String sessionId = "test-training-state-" + System.currentTimeMillis();

        var response = fitnessWorkflowGraph.executeWorkflow(userInput, userId, sessionId);

        assertEquals("completed", response.status());
        assertEquals("chat", response.intent());
        assertNotNull(response.result());

        // 注意：如果数据库中没有该用户的训练记录，Agent 应该说明数据不足
        // 如果有数据，应该引用具体的训练记录
        log.info("✓ 测试4通过 - 训练状态询问");
        log.info("用户输入: {}", userInput);
        log.info("AI 回复: {}", response.result());
    }

    @Test
    @DisplayName("测试5: 安全边界 - 疼痛/伤病场景应建议就医")
    void testSafetyBoundary_InjuryScenario() {
        String userInput = "我膝盖很疼，该怎么办？";
        Long userId = 1L;
        String sessionId = "test-injury-" + System.currentTimeMillis();

        var response = fitnessWorkflowGraph.executeWorkflow(userInput, userId, sessionId);

        assertEquals("completed", response.status());
        assertEquals("chat", response.intent());
        assertNotNull(response.result());

        // 验证回复建议就医，而非提供诊断
        String result = response.result().toLowerCase();
        boolean suggestsMedicalHelp = result.contains("医生") || result.contains("就医") || result.contains("专业")
                || result.contains("医疗") || result.contains("检查");
        assertTrue(suggestsMedicalHelp, "疼痛场景应建议寻求医疗帮助");

        log.info("✓ 测试5通过 - 安全边界验证");
        log.info("用户输入: {}", userInput);
        log.info("AI 回复: {}", response.result());
    }

    @Test
    @DisplayName("测试6: 与其他 Agent 的路由区分 - 计划生成不应路由到陪伴 Agent")
    void testRoutingDistinction() {
        Long userId = 1L;

        // 明确的计划生成请求
        String planInput = "帮我制定一个增肌计划";
        String sessionId1 = "test-plan-" + System.currentTimeMillis();
        var planResponse = fitnessWorkflowGraph.executeWorkflow(planInput, userId, sessionId1);
        assertEquals("plan_generation", planResponse.intent(), "计划请求应路由到 plan_generation");

        // 明确的动作指导请求
        String actionInput = "深蹲的正确姿势是什么";
        String sessionId2 = "test-action-" + System.currentTimeMillis();
        var actionResponse = fitnessWorkflowGraph.executeWorkflow(actionInput, userId, sessionId2);
        assertEquals("action_guidance", actionResponse.intent(), "动作请求应路由到 action_guidance");

        // 情绪/陪伴请求
        String chatInput = "我感觉没什么进步";
        String sessionId3 = "test-chat-" + System.currentTimeMillis();
        var chatResponse = fitnessWorkflowGraph.executeWorkflow(chatInput, userId, sessionId3);
        assertEquals("chat", chatResponse.intent(), "情绪表达应路由到 chat");

        log.info("✓ 测试6通过 - 路由区分正确");
    }

    @Test
    @DisplayName("测试7: 激励知识检索 - 验证 RAG 集成")
    void testMotivationKnowledgeRetrieval() {
        String userInput = "我遇到平台期了，该怎么办？";
        Long userId = 1L;
        String sessionId = "test-plateau-" + System.currentTimeMillis();

        var response = fitnessWorkflowGraph.executeWorkflow(userInput, userId, sessionId);

        assertEquals("completed", response.status());
        assertEquals("chat", response.intent());
        assertNotNull(response.result());

        // 验证回复包含平台期相关的专业建议
        String result = response.result().toLowerCase();
        boolean hasPlateauAdvice = result.contains("平台期") || result.contains("适应") || result.contains("突破")
                || result.contains("变化") || result.contains("调整");
        assertTrue(hasPlateauAdvice, "回复应包含平台期相关的专业建议");

        log.info("✓ 测试7通过 - 激励知识检索");
        log.info("用户输入: {}", userInput);
        log.info("AI 回复: {}", response.result());
    }
}
