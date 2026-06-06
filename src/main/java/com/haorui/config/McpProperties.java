package com.haorui.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP服务配置属性
 * 
 * 配置示例：
 * agentscope.mcp.enabled=true
 * agentscope.mcp.request-timeout-seconds=120
 * agentscope.mcp.servers[0].name=filesystem
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "agentscope.mcp")
public class McpProperties {

    /**
     * 是否启用MCP
     */
    private boolean enabled = false;

    /**
     * MCP请求超时时间（秒）
     */
    @Positive
    private int requestTimeoutSeconds = 120;

    /**
     * MCP初始化超时时间（秒）
     */
    @Positive
    private int initTimeoutSeconds = 30;

    /**
     * MCP服务器配置列表
     */
    private List<McpServerConfig> servers = new ArrayList<>();

    @Data
    @Validated
    public static class McpServerConfig {
        
        /**
         * 服务器名称（唯一标识）
         */
        @NotBlank(message = "服务器名称不能为空")
        private String name;

        /**
         * 传输类型：stdio, sse, http
         */
        private String transport = "stdio";

        /**
         * StdIO命令（如：npx, python）
         * 仅用于stdio传输
         */
        private String command;

        /**
         * StdIO参数列表
         */
        private List<String> args = new ArrayList<>();

        /**
         * HTTP/SSE端点URL
         * 仅用于sse/http传输
         */
        private String endpoint;

        /**
         * 是否默认激活
         */
        private boolean active = true;

        /**
         * 启用的工具列表（白名单）
         * 空列表表示启用所有工具
         */
        private List<String> enableTools = new ArrayList<>();

        /**
         * 禁用的工具列表（黑名单）
         */
        private List<String> disableTools = new ArrayList<>();

        /**
         * 获取传输类型描述
         */
        public String getTransportDescription() {
            return switch (transport.toLowerCase()) {
                case "stdio" -> "本地进程 (" + command + ")";
                case "sse" -> "SSE流式 (" + endpoint + ")";
                case "http" -> "HTTP请求 (" + endpoint + ")";
                default -> "未知传输";
            };
        }
    }
}