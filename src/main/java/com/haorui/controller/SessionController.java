package com.haorui.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理控制器
 * 生产场景下由服务端统一管理 sessionId，客户端无需自己生成
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    /** 简单的内存会话注册表（生产环境替换为 Redis） */
    private final Map<String, SessionMeta> sessions = new ConcurrentHashMap<>();

    /**
     * 创建新会话，返回 sessionId
     * 前端在开始对话前调用一次，后续请求带上返回的 sessionId 即可
     *
     * POST /api/sessions
     * 可选 Body: { "userId": "alice", "agentType": "tool" }
     */
    @PostMapping
    public Map<String, Object> createSession(
            @RequestBody(required = false) Map<String, String> body) {

        String sessionId = UUID.randomUUID().toString();
        String userId = body != null ? body.getOrDefault("userId", "anonymous") : "anonymous";
        String agentType = body != null ? body.getOrDefault("agentType", "default") : "default";

        sessions.put(sessionId, new SessionMeta(sessionId, userId, agentType, Instant.now()));
        log.info("Session created: sessionId={}, userId={}, agentType={}", sessionId, userId, agentType);

        return Map.of(
                "sessionId", sessionId,
                "userId", userId,
                "agentType", agentType,
                "createdAt", Instant.now().toString(),
                "tip", "后续请求在 body 中携带此 sessionId 以保持对话上下文"
        );
    }

    /**
     * 查询会话信息
     * GET /api/sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public Map<String, Object> getSession(@PathVariable String sessionId) {
        SessionMeta meta = sessions.get(sessionId);
        if (meta == null) {
            return Map.of("exists", false, "sessionId", sessionId);
        }
        return Map.of(
                "exists", true,
                "sessionId", meta.sessionId(),
                "userId", meta.userId(),
                "agentType", meta.agentType(),
                "createdAt", meta.createdAt().toString()
        );
    }

    /**
     * 删除会话（退出对话）
     * DELETE /api/sessions/{sessionId}
     */
    @DeleteMapping("/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId) {
        boolean removed = sessions.remove(sessionId) != null;
        log.info("Session deleted: sessionId={}, existed={}", sessionId, removed);
        return Map.of("success", removed, "sessionId", sessionId);
    }

    /**
     * 会话元数据（内存存储，生产环境替换为 Redis Hash）
     */
    private record SessionMeta(
            String sessionId,
            String userId,
            String agentType,
            Instant createdAt
    ) {}
}

