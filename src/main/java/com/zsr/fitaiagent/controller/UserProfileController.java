package com.zsr.fitaiagent.controller;

import com.zsr.fitaiagent.agent.UserProfileAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 用户画像 Controller
 */
@RestController
@RequestMapping("/user-profile")
@Tag(name = "用户画像", description = "用户画像生成相关接口")
@Slf4j
public class UserProfileController {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    @Qualifier("allTools")
    private ToolCallback[] allTools;

    @Autowired
    @Qualifier("mcpTools")
    private ToolCallback[] mcpTools;

    @GetMapping("/generate/{userId}")
    @Operation(summary = "生成用户画像", description = "根据用户ID生成完整的用户画像报告")
    public String generateUserProfile(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("开始生成用户画像，用户ID: {}", userId);

        // 创建 ChatClient
        ChatClient chatClient = chatClientBuilder.build();

        // 创建 UserProfileAgent，使用所有工具（包括 MCP 工具）
        UserProfileAgent agent = new UserProfileAgent(allTools, chatClient);

        // 生成用户画像
        String result = agent.generateUserProfile(userId);

        log.info("用户画像生成完成");
        return result;
    }

    @GetMapping("/generate-stream/{userId}")
    @Operation(summary = "生成用户画像（流式）", description = "根据用户ID生成完整的用户画像报告，使用SSE流式输出")
    public SseEmitter generateUserProfileStream(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("开始生成用户画像（流式），用户ID: {}", userId);

        // 创建 ChatClient
        ChatClient chatClient = chatClientBuilder.build();

        // 创建 UserProfileAgent
        UserProfileAgent agent = new UserProfileAgent(allTools, chatClient);

        // 构建提示词
        String userPrompt = String.format(
                "请为用户ID=%d生成完整的用户画像。" +
                        "首先查询用户基本信息，然后查询健身记录，最后生成画像报告。",
                userId
        );

        // 流式生成用户画像
        return agent.runStream(userPrompt);
    }

    @GetMapping("/tools/list")
    @Operation(summary = "查看可用工具", description = "列出所有可用的工具（本地工具 + MCP工具）")
    public String listTools() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 所有可用工具 ===\n");
        sb.append("总数: ").append(allTools.length).append("\n\n");

        for (int i = 0; i < allTools.length; i++) {
            ToolCallback tool = allTools[i];
            sb.append(i + 1).append(". ")
                    .append(tool.getToolDefinition().name())
                    .append(" - ")
                    .append(tool.getToolDefinition().description())
                    .append("\n");
        }

        sb.append("\n=== MCP 工具 ===\n");
        sb.append("总数: ").append(mcpTools.length).append("\n\n");

        for (int i = 0; i < mcpTools.length; i++) {
            ToolCallback tool = mcpTools[i];
            sb.append(i + 1).append(". ")
                    .append(tool.getToolDefinition().name())
                    .append(" - ")
                    .append(tool.getToolDefinition().description())
                    .append("\n");
        }

        return sb.toString();
    }
}
