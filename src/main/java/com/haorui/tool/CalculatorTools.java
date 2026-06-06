package com.haorui.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * 计算工具组
 */
@Slf4j
@Component
public class CalculatorTools {

    @Tool(name = "calculate", description = "执行数学计算，支持加减乘除、括号和幂运算")
    public String calculate(
            @ToolParam(name = "expression", description = "数学表达式，如 2+3*4 或 (2+3)*4 或 2^10") String expression) {
        try {
            Expression exp = new ExpressionBuilder(expression).build();
            ValidationResult validation = exp.validate();
            if (!validation.isValid()) {
                return "表达式无效: " + String.join(", ", validation.getErrors());
            }
            double result = exp.evaluate();
            // 整数结果去掉小数点
            if (result == Math.floor(result) && !Double.isInfinite(result) && Math.abs(result) < 1e15) {
                return "计算结果: " + (long) result;
            }
            return "计算结果: " + result;
        } catch (Exception e) {
            log.error("计算失败: expression={}", expression, e);
            return "计算失败，请检查表达式格式（支持: + - * / ^ 括号）";
        }
    }

    @Tool(name = "convert_unit", description = "单位转换")
    public String convertUnit(
            @ToolParam(name = "value", description = "数值") double value,
            @ToolParam(name = "from_unit", description = "源单位：km, m, cm, mm, kg, g, mg") String fromUnit,
            @ToolParam(name = "to_unit", description = "目标单位：km, m, cm, mm, kg, g, mg") String toUnit) {

        // 归一化到基准单位（长度:m，重量:kg）
        double baseValue;
        String baseType;

        switch (fromUnit.toLowerCase()) {
            case "km"  -> { baseValue = value * 1000;   baseType = "length"; }
            case "m"   -> { baseValue = value;          baseType = "length"; }
            case "cm"  -> { baseValue = value / 100;    baseType = "length"; }
            case "mm"  -> { baseValue = value / 1000;   baseType = "length"; }
            case "kg"  -> { baseValue = value;          baseType = "weight"; }
            case "g"   -> { baseValue = value / 1000;   baseType = "weight"; }
            case "mg"  -> { baseValue = value / 1_000_000; baseType = "weight"; }
            default -> { return "不支持的源单位: " + fromUnit; }
        }

        double result;
        switch (toUnit.toLowerCase()) {
            case "km"  -> { if (!"length".equals(baseType)) return "单位类型不匹配"; result = baseValue / 1000; }
            case "m"   -> { if (!"length".equals(baseType)) return "单位类型不匹配"; result = baseValue; }
            case "cm"  -> { if (!"length".equals(baseType)) return "单位类型不匹配"; result = baseValue * 100; }
            case "mm"  -> { if (!"length".equals(baseType)) return "单位类型不匹配"; result = baseValue * 1000; }
            case "kg"  -> { if (!"weight".equals(baseType)) return "单位类型不匹配"; result = baseValue; }
            case "g"   -> { if (!"weight".equals(baseType)) return "单位类型不匹配"; result = baseValue * 1000; }
            case "mg"  -> { if (!"weight".equals(baseType)) return "单位类型不匹配"; result = baseValue * 1_000_000; }
            default -> { return "不支持的目标单位: " + toUnit; }
        }

        if (result == Math.floor(result) && !Double.isInfinite(result)) {
            return (long) result + " " + toUnit;
        }
        return result + " " + toUnit;
    }
}