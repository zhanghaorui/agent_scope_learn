package com.haorui.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AgentScope配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "agentscope.ollama")
public class OllamaProperties {

    /**
     * Ollama服务地址
     */
    private String baseUrl = "http://localhost:11434";

    /**
     * 模型名称，如 qwen3:8b, llama3.2, mistral
     */
    private String model = "qwen3:8b";

    /**
     * Agent名称
     */
    private String agentName = "ollama-assistant";

    /**
     * 系统提示词
     */
    private String sysPrompt = "你是一个友好的AI助手，使用中文回答问题。";

    /**
     * 工作区路径
     */
    private String workspacePath = ".agentscope/workspace";

    /**
     * 默认用户ID
     */
    private String defaultUserId = "user";
}