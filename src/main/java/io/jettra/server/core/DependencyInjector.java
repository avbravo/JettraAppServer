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
            } else if (field.isAnnotationPresent(io.jettra.core.inject.annotation.InjectProperties.class)) {
                io.jettra.core.inject.annotation.InjectProperties propsMeta = field.getAnnotation(io.jettra.core.inject.annotation.InjectProperties.class);
                String name = propsMeta.name();
                String langSuffix = "";
                JettraContext context = JettraContext.getCurrent();
                if (context != null) {
                    Object sessionLang = context.get(JettraContext.Scope.SESSION, "jettra_lang");
                    if (sessionLang != null && sessionLang.toString().length() > 0) {
                        langSuffix = "_" + sessionLang.toString();
                    }
                }
                
                java.util.Properties properties = new java.util.Properties();
                String targetProp = name + langSuffix + ".properties";
                try (java.io.InputStream is = clazz.getClassLoader().getResourceAsStream(targetProp)) {
                    if (is != null) {
                        properties.load(is);
                    } else {
                        java.io.InputStream isDef = clazz.getClassLoader().getResourceAsStream(name + ".properties");
                        if (isDef != null) properties.load(isDef);
                        else {
                            java.io.InputStream is2 = clazz.getClassLoader().getResourceAsStream(name + "_es.properties");
                            if (is2 != null) properties.load(is2);
                        }
                    }
                    
                    field.setAccessible(true);
                    field.set(target, properties);
                } catch (Exception e) {
                    System.err.println("[DependencyInjector] Error loading properties " + name + ": " + e.getMessage());
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
