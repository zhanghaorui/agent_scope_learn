package com.haorui;

import com.haorui.dto.ChatRequest;
import com.haorui.util.AgentContextUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AgentContextUtils 单元测试
 */
@DisplayName("Agent上下文工具测试")
class AgentContextUtilsTest {

    @Test
    @DisplayName("sessionId 为 null 时自动生成 UUID")
    void testResolveSessionId_generatesUuidWhenNull() {
        ChatRequest req = ChatRequest.builder().message("hi").build();
        String id = AgentContextUtils.resolveSessionId(req);
        assertThat(id).isNotBlank();
        // UUID 格式
        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("sessionId 为空白字符串时自动生成 UUID")
    void testResolveSessionId_generatesUuidWhenBlank() {
        ChatRequest req = ChatRequest.builder().message("hi").sessionId("   ").build();
        String id = AgentContextUtils.resolveSessionId(req);
        assertThat(id).isNotBlank().doesNotContain(" ");
    }

    @Test
    @DisplayName("sessionId 有值时原样返回")
    void testResolveSessionId_returnsExistingId() {
        ChatRequest req = ChatRequest.builder().message("hi").sessionId("my-session").build();
        assertThat(AgentContextUtils.resolveSessionId(req)).isEqualTo("my-session");
    }

    @Test
    @DisplayName("userId 为 null 时使用默认值")
    void testResolveUserId_usesDefault() {
        ChatRequest req = ChatRequest.builder().message("hi").build();
        assertThat(AgentContextUtils.resolveUserId(req, "default-user")).isEqualTo("default-user");
    }

    @Test
    @DisplayName("userId 有值时原样返回")
    void testResolveUserId_returnsExistingId() {
        ChatRequest req = ChatRequest.builder().message("hi").userId("alice").build();
        assertThat(AgentContextUtils.resolveUserId(req, "default-user")).isEqualTo("alice");
    }
}

