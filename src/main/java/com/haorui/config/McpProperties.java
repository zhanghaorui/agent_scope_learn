package com.haorui.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP服务配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "agentscope.mcp")
public class McpProperties {

    /**
     * 是否启用MCP
     */
    private boolean enabled = false;

    /**
     * MCP服务器配置列表
     */
    private List<McpServerConfig> servers = new ArrayList<>();

    @Data
    public static class McpServerConfig {
        /**
         * 服务器名称
         */
        private String name;

        /**
         * 传输类型：stdio, sse, http
         */
        private String transport = "stdio";

        /**
         * StdIO命令（如：npx, python）
         */
        private String command;

        /**
         * StdIO参数
         */
        private List<String> args = new ArrayList<>();

        /**
         * HTTP/SSE端点URL
         */
        private String endpoint;

        /**
         * 工作目录（用于filesystem-mcp）
         */
        private String workingDir;

        /**
         * 是否默认激活
         */
        private boolean active = true;

        /**
         * 启用的工具列表（白名单）
         */
        private List<String> enableTools = new ArrayList<>();

        /**
         * 禁用的工具列表（黑名单）
         */
        private List<String> disableTools = new ArrayList<>();
    }
}