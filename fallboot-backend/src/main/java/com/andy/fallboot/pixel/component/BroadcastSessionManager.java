package com.andy.fallboot.pixel.component;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class BroadcastSessionManager {

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), new SessionEntry(session, new ReentrantLock()));
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    public void markReady(String sessionId) {
        var entry = sessions.get(sessionId);
        if (entry != null) {
            entry.ready = true;
        }
    }

    public void broadcast(String roomId, String jsonPayload) {
        TextMessage message = new TextMessage(jsonPayload);
        sessions.forEach((id, entry) -> {
            if (!entry.ready) return;
            if (!Objects.equals(roomId, entry.session.getAttributes().get("roomId"))) return;
            if (!entry.session.isOpen()) {
                sessions.remove(id);
                return;
            }
            Thread.startVirtualThread(() -> {
                entry.lock.lock();
                try {
                    entry.session.sendMessage(message);
                } catch (IOException | IllegalStateException e) {
                    sessions.remove(id);
                } finally {
                    entry.lock.unlock();
                }
            });
        });
    }

    private static class SessionEntry {
        final WebSocketSession session;
        final ReentrantLock lock;
        volatile boolean ready;

        SessionEntry(WebSocketSession session, ReentrantLock lock) {
            this.session = session;
            this.lock = lock;
        }
    }
}
