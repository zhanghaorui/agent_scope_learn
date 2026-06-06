package com.haorui.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文本工具组
 */
@Slf4j
@Component
public class TextTools {

    @Tool(name = "count_characters", description = "统计文本字符数")
    public String countCharacters(
            @ToolParam(name = "text", description = "要统计的文本") String text) {
        if (text == null) return "文本为空";
        return "总字符数: " + text.length() + "，中文字符: " + text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
    }

    @Tool(name = "reverse_text", description = "反转文本")
    public String reverseText(
            @ToolParam(name = "text", description = "要反转的文本") String text) {
        if (text == null) return "文本为空";
        return new StringBuilder(text).reverse().toString();
    }

    @Tool(name = "extract_numbers", description = "从文本中提取所有数字")
    public String extractNumbers(
            @ToolParam(name = "text", description = "包含数字的文本") String text) {
        if (text == null) return "文本为空";
        String numbers = text.replaceAll("[^0-9.\\-]", " ").trim().replaceAll(" +", ",");
        return numbers.isEmpty() ? "未找到数字" : "提取的数字: " + numbers;
    }
}