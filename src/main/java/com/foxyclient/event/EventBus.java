package com.foxyclient.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publish/subscribe event bus with priority support.
 */
public class EventBus {
    private final Map<Class<? extends Event>, List<Listener>> listeners = new ConcurrentHashMap<>();

    private record Listener(Object instance, Method method, int priority) implements Comparable<Listener> {
        @Override
        public int compareTo(Listener other) {
            return Integer.compare(other.priority, this.priority);
        }
    }

    public void register(Object subscriber) {
        for (Method method : subscriber.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            if (method.getParameterCount() != 1) continue;

            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) paramType;
            EventHandler annotation = method.getAnnotation(EventHandler.class);

            method.setAccessible(true);
            List<Listener> list = listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
            list.add(new Listener(subscriber, method, annotation.priority()));
            list.sort(Comparator.naturalOrder());
        }
    }

    public void unregister(Object subscriber) {
        for (List<Listener> list : listeners.values()) {
            list.removeIf(l -> l.instance == subscriber);
        }
    }

    public <T extends Event> T post(T event) {
        List<Listener> list = listeners.get(event.getClass());
        if (list == null) return event;

        for (Listener listener : list) {
            try {
                listener.method.invoke(listener.instance, event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return event;
    }
}
