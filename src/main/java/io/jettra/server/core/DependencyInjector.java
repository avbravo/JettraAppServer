package io.jettra.server.core;

import io.jettra.core.inject.annotation.Inject;
import io.jettra.scoped.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles dependency injection for instances managed by JettraServer.
 */
public class DependencyInjector {

    // Simple registry for un-scoped singletons if they don't have scope annotations
    private static final Map<Class<?>, Object> singletonRegistry = new ConcurrentHashMap<>();

    public static void inject(Object target) {
        if (target == null) return;
        
        Class<?> clazz = target.getClass();
        
        // Traverse class hierarchy if necessary, but here we just do declared fields
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                Class<?> fieldType = field.getType();
                Object dependency = resolveDependency(fieldType);
                
                if (dependency != null) {
                    field.setAccessible(true);
                    try {
                        field.set(target, dependency);
                    } catch (IllegalAccessException e) {
                        System.err.println("[DependencyInjector] Error injecting field " + field.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
    
    private static Object resolveDependency(Class<?> type) {
        JettraContext context = JettraContext.getCurrent();
        
        // Determine scope
        if (type.isAnnotationPresent(SessionScoped.class)) {
            if (context == null) return instantiate(type);
            Object obj = context.get(JettraContext.Scope.SESSION, type.getName());
            if (obj == null) {
                obj = instantiate(type);
                context.set(JettraContext.Scope.SESSION, type.getName(), obj);
            }
            return obj;
        } else if (type.isAnnotationPresent(ApplicationScoped.class)) {
            if (context == null) return instantiate(type);
            Object obj = context.get(JettraContext.Scope.APPLICATION, type.getName());
            if (obj == null) {
                obj = instantiate(type);
                context.set(JettraContext.Scope.APPLICATION, type.getName(), obj);
            }
            return obj;
        } else if (type.isAnnotationPresent(RequestScoped.class)) {
            if (context == null) return instantiate(type);
            Object obj = context.get(JettraContext.Scope.REQUEST, type.getName());
            if (obj == null) {
                obj = instantiate(type);
                context.set(JettraContext.Scope.REQUEST, type.getName(), obj);
            }
            return obj;
        } else if (type.isAnnotationPresent(ViewScoped.class)) {
            if (context == null) return instantiate(type);
            Object obj = context.get(JettraContext.Scope.VIEW, type.getName());
            if (obj == null) {
                obj = instantiate(type);
                context.set(JettraContext.Scope.VIEW, type.getName(), obj);
            }
            return obj;
        } else if (type.isAnnotationPresent(WindowScoped.class)) {
            if (context == null) return instantiate(type);
            Object obj = context.get(JettraContext.Scope.WINDOW, type.getName());
            if (obj == null) {
                obj = instantiate(type);
                context.set(JettraContext.Scope.WINDOW, type.getName(), obj);
            }
            return obj;
        } else if (type.isAnnotationPresent(ClientScoped.class)) {
            if (context == null) return instantiate(type);
            Object obj = context.get(JettraContext.Scope.CLIENT, type.getName());
            if (obj == null) {
                obj = instantiate(type);
                context.set(JettraContext.Scope.CLIENT, type.getName(), obj);
            }
            return obj;
        }
        
        // Default to ApplicationScoped singleton behavior if no scope is defined
        return singletonRegistry.computeIfAbsent(type, DependencyInjector::instantiate);
    }
    
    private static Object instantiate(Class<?> type) {
        try {
            // Attempt to instantiate interface implementations if needed
            // For now, assume it's a concrete class with no-arg constructor
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            System.err.println("[DependencyInjector] Error instantiating " + type.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
