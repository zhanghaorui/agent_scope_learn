package com.haorui.tool;

import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolResultBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具组
 */
@Slf4j
@Component
public class TimeTools {

    @Tool(name = "get_current_time", description = "获取当前时间")
    public String getCurrentTime(
            @ToolParam(name = "format", description = "时间格式，如 yyyy-MM-dd HH:mm:ss") String format) {
        String pattern = (format == null || format.isBlank()) ? "yyyy-MM-dd HH:mm:ss" : format;
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    @Tool(name = "get_weekday", description = "获取今天是星期几")
    public String getWeekday() {
        String[] weekdays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        int dayOfWeek = LocalDateTime.now().getDayOfWeek().getValue() - 1;
        return weekdays[dayOfWeek];
    }
}