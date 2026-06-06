package com.haorui.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 计算工具组
 */
@Slf4j
@Component
public class CalculatorTools {

    @Tool(name = "calculate", description = "执行数学计算")
    public String calculate(
            @ToolParam(name = "expression", description = "数学表达式，如 2+3*4") String expression) {
        try {
            // 简单计算器实现（仅支持加减乘除）
            expression = expression.replaceAll(" ", "");
            
            // 处理乘除
            while (expression.contains("*") || expression.contains("/")) {
                int mulIdx = expression.indexOf("*");
                int divIdx = expression.indexOf("/");
                int opIdx = (mulIdx >= 0 && (divIdx < 0 || mulIdx < divIdx)) ? mulIdx : divIdx;
                
                if (opIdx < 0) break;
                
                String left = findNumberLeft(expression, opIdx);
                String right = findNumberRight(expression, opIdx);
                double result = expression.charAt(opIdx) == '*' 
                        ? Double.parseDouble(left) * Double.parseDouble(right)
                        : Double.parseDouble(left) / Double.parseDouble(right);
                
                expression = expression.substring(0, opIdx - left.length()) 
                        + result 
                        + expression.substring(opIdx + right.length() + 1);
            }
            
            // 处理加减
            while (expression.contains("+") || (expression.lastIndexOf("-") > 0)) {
                int addIdx = expression.indexOf("+");
                int subIdx = expression.lastIndexOf("-");
                // 避免匹配到负号
                if (subIdx > 0 && expression.charAt(subIdx - 1) >= '0' && expression.charAt(subIdx - 1) <= '9') {
                    // 是减号
                } else {
                    subIdx = -1;
                }
                
                int opIdx = (addIdx >= 0 && (subIdx < 0 || addIdx < subIdx)) ? addIdx : subIdx;
                if (opIdx < 0) break;
                
                String left = findNumberLeft(expression, opIdx);
                String right = findNumberRight(expression, opIdx);
                double result = expression.charAt(opIdx) == '+' 
                        ? Double.parseDouble(left) + Double.parseDouble(right)
                        : Double.parseDouble(left) - Double.parseDouble(right);
                
                expression = expression.substring(0, opIdx - left.length()) 
                        + result 
                        + expression.substring(opIdx + right.length() + 1);
            }
            
            return "计算结果: " + expression;
        } catch (Exception e) {
            log.error("计算失败: {}", expression, e);
            return "计算失败，请检查表达式格式";
        }
    }
    
    private String findNumberLeft(String expr, int opIdx) {
        int start = opIdx - 1;
        while (start >= 0 && (expr.charAt(start) >= '0' && expr.charAt(start) <= '9' || expr.charAt(start) == '.' || expr.charAt(start) == '-')) {
            start--;
        }
        return expr.substring(start + 1, opIdx);
    }
    
    private String findNumberRight(String expr, int opIdx) {
        int end = opIdx + 1;
        while (end < expr.length() && (expr.charAt(end) >= '0' && expr.charAt(end) <= '9' || expr.charAt(end) == '.' || expr.charAt(end) == '-')) {
            end++;
        }
        return expr.substring(opIdx + 1, end);
    }

    @Tool(name = "convert_unit", description = "单位转换")
    public String convertUnit(
            @ToolParam(name = "value", description = "数值") double value,
            @ToolParam(name = "from_unit", description = "源单位：km, m, cm, kg, g") String fromUnit,
            @ToolParam(name = "to_unit", description = "目标单位：km, m, cm, kg, g") String toUnit) {
        
        // 长度转换
        if (fromUnit.equals("km") && toUnit.equals("m")) return value * 1000 + " m";
        if (fromUnit.equals("m") && toUnit.equals("km")) return value / 1000 + " km";
        if (fromUnit.equals("m") && toUnit.equals("cm")) return value * 100 + " cm";
        if (fromUnit.equals("cm") && toUnit.equals("m")) return value / 100 + " m";
        
        // 重量转换
        if (fromUnit.equals("kg") && toUnit.equals("g")) return value * 1000 + " g";
        if (fromUnit.equals("g") && toUnit.equals("kg")) return value / 1000 + " kg";
        
        return "不支持该转换: " + fromUnit + " -> " + toUnit;
    }
}