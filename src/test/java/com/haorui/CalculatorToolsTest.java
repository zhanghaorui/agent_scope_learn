package com.haorui;

import com.haorui.tool.CalculatorTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CalculatorTools 单元测试
 */
@DisplayName("计算工具测试")
class CalculatorToolsTest {

    private CalculatorTools calculator;

    @BeforeEach
    void setUp() {
        calculator = new CalculatorTools();
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({
            "2+3,           计算结果: 5",
            "10-4,          计算结果: 6",
            "3*4,           计算结果: 12",
            "10/4,          计算结果: 2.5",
            "(2+3)*4,       计算结果: 20",
            "2^10,          计算结果: 1024",
            "2+3*4,         计算结果: 14",     // 运算符优先级
    })
    @DisplayName("基本表达式计算")
    void testCalculate(String expression, String expected) {
        assertThat(calculator.calculate(expression)).isEqualTo(expected);
    }

    @Test
    @DisplayName("非法表达式返回失败提示")
    void testInvalidExpression() {
        String result = calculator.calculate("abc+1");
        assertThat(result).contains("计算失败");
    }

    @ParameterizedTest(name = "{0} {1} -> {2} = {3}")
    @CsvSource({
            "1.0,   km, m,  1000 m",
            "500.0, m,  km, 0.5 km",
            "1.0,   m,  cm, 100 cm",
            "1.0,   kg, g,  1000 g",
            "500.0, g,  kg, 0.5 kg",
    })
    @DisplayName("单位转换")
    void testConvertUnit(double value, String from, String to, String expected) {
        assertThat(calculator.convertUnit(value, from, to)).isEqualTo(expected);
    }

    @Test
    @DisplayName("跨类型单位转换返回错误提示")
    void testConvertUnitTypeMismatch() {
        assertThat(calculator.convertUnit(1.0, "km", "kg")).contains("单位类型不匹配");
    }
}

