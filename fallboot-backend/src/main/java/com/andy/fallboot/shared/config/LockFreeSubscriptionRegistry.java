package com.andy.fallboot.shared.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.broker.SubscriptionRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import java.util.concurrent.ConcurrentHashMap;

public class LockFreeSubscriptionRegistry implements SubscriptionRegistry {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> sessionSubs = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> destinationSubs = new ConcurrentHashMap<>();

    @Override
    public void registerSubscription(Message<?> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return;

        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        String destination = accessor.getDestination();
        if (sessionId == null || subscriptionId == null || destination == null) return;

        sessionSubs.computeIfAbsent(sessionId, _ -> new ConcurrentHashMap<>())
                .put(subscriptionId, destination);

        destinationSubs.computeIfAbsent(destination, _ -> new ConcurrentHashMap<>())
                .put(sessionId + ":" + subscriptionId, Boolean.TRUE);
    }

    @Override
    public void unregisterSubscription(Message<?> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return;

        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        if (sessionId == null || subscriptionId == null) return;

        var subs = sessionSubs.get(sessionId);
        if (subs != null) {
            String destination = subs.remove(subscriptionId);
            if (destination != null) {
                var destSubs = destinationSubs.get(destination);
                if (destSubs != null) {
                    destSubs.remove(sessionId + ":" + subscriptionId);
                    if (destSubs.isEmpty()) {
                        destinationSubs.remove(destination);
                    }
                }
            }
            if (subs.isEmpty()) {
                sessionSubs.remove(sessionId);
            }
        }
    }

    @Override
    public void unregisterAllSubscriptions(String sessionId) {
        var subs = sessionSubs.remove(sessionId);
        if (subs != null) {
            subs.forEach((subscriptionId, destination) -> {
                var destSubs = destinationSubs.get(destination);
                if (destSubs != null) {
                    destSubs.remove(sessionId + ":" + subscriptionId);
                    if (destSubs.isEmpty()) {
                        destinationSubs.remove(destination);
                    }
                }
            });
        }
    }

    @Override
    public MultiValueMap<String, String> findSubscriptions(Message<?> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return new LinkedMultiValueMap<>();

        String destination = accessor.getDestination();
        if (destination == null) return new LinkedMultiValueMap<>();

        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        var destSubs = destinationSubs.get(destination);
        if (destSubs != null) {
            destSubs.keySet().forEach(key -> {
                String[] parts = key.split(":", 2);
                if (parts.length == 2) {
                    result.add(parts[0], parts[1]);
                }
            });
        }
        return result;
    }
}
