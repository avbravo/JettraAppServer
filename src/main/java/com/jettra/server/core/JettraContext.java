package com.jettra.server.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages different scopes (Request, Session, Application, etc.) for Jettra applications.
 */
public class JettraContext {

    public enum Scope {
        REQUEST, SESSION, APPLICATION, VIEW, WINDOW, CLIENT, CACHE
    }

    private static final ThreadLocal<JettraContext> currentContext = new ThreadLocal<>();
    private static final Map<String, Map<String, Object>> applicationScope = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    private final Map<Scope, Map<String, Object>> localScopes = new ConcurrentHashMap<>();
    private final String sessionId;

    public JettraContext(String sessionId) {
        this.sessionId = sessionId;
        Map<String, Object> reqMap = new ConcurrentHashMap<>();
        reqMap.put("MAP", new ConcurrentHashMap<String, Object>());
        localScopes.put(Scope.REQUEST, reqMap);
        
        sessions.computeIfAbsent(sessionId, k -> {
            Map<String, Object> sessMap = new ConcurrentHashMap<>();
            sessMap.put("MAP", new ConcurrentHashMap<String, Object>());
            return sessMap;
        });
    }

    public static void setCurrent(JettraContext context) {
        currentContext.set(context);
    }

    public static JettraContext getCurrent() {
        return currentContext.get();
    }

    public static void clear() {
        currentContext.remove();
    }

    public Object get(Scope scope, String key) {
        switch (scope) {
            case APPLICATION:
                return applicationScope.getOrDefault("global", new ConcurrentHashMap<>()).get(key);
            case SESSION:
                return sessions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).get(key);
            default:
                return localScopes.getOrDefault(scope, new ConcurrentHashMap<>()).get(key);
        }
    }

    public void set(Scope scope, String key, Object value) {
        switch (scope) {
            case APPLICATION:
                applicationScope.computeIfAbsent("global", k -> new ConcurrentHashMap<>()).put(key, value);
                break;
            case SESSION:
                sessions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(key, value);
                break;
            default:
                localScopes.computeIfAbsent(scope, k -> new ConcurrentHashMap<>()).put(key, value);
                break;
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * Clears all active sessions from the server-side registry.
     */
    public static void clearSessions() {
        sessions.clear();
    }

    public static int getActiveSessionCount() {
        return sessions.size();
    }

    public static Map<String, Map<String, Object>> getSessions() {
        return sessions;
    }
}
