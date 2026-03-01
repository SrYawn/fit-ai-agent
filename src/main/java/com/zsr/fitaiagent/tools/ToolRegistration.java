package com.zsr.fitaiagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 集中的工具注册类
 */
@Configuration
@Slf4j
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 本地工具
     */
    @Bean
    public ToolCallback[] localTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();

        ToolCallback[] localTools = MethodToolCallbackProvider.builder().toolObjects(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        ).build().getToolCallbacks();
        logToolNames("local", localTools);
        return localTools;
    }

    /**
     * MCP 工具
     */
    @Bean
    public ToolCallback[] mcpTools() {
        if (toolCallbackProvider != null) {
            ToolCallback[] mcpTools = toolCallbackProvider.getToolCallbacks();
            if (mcpTools != null && mcpTools.length > 0) {
                logToolNames("mcp", mcpTools);
                return mcpTools;
            }
        }
        log.info("Registered mcp tools (0): []");
        return new ToolCallback[0];
    }

    /**
     * 所有工具（本地 + MCP）
     */
    @Bean
    public ToolCallback[] allTools() {
        List<ToolCallback> toolList = new ArrayList<>();

        // 添加本地工具
        ToolCallback[] local = localTools();
        for (ToolCallback tool : local) {
            toolList.add(tool);
        }

        // 添加 MCP 工具
        ToolCallback[] mcp = mcpTools();
        for (ToolCallback tool : mcp) {
            toolList.add(tool);
        }

        ToolCallback[] allTools = toolList.toArray(new ToolCallback[0]);
        logToolNames("all", allTools);
        return allTools;
    }

    private void logToolNames(String group, ToolCallback[] tools) {
        String names = java.util.Arrays.stream(tools)
                .map(tool -> tool.getToolDefinition().name())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        log.info("Registered {} tools ({}): [{}]", group, tools.length, names);
    }
}
