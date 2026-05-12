package com.motorcycle.repair.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ShopWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> shopSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long shopId = extractShopId(session);
        if (shopId != null) {
            shopSessions.put(shopId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long shopId = extractShopId(session);
        if (shopId != null) {
            shopSessions.remove(shopId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    }

    public void pushToShop(Long shopId, Object notification) {
        WebSocketSession session = shopSessions.get(shopId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(json));
            } catch (IOException ignored) {}
        }
    }

    private Long extractShopId(WebSocketSession session) {
        try {
            String uri = session.getUri().getPath();
            UriTemplate template = new UriTemplate("/ws/shop/{shopId}");
            Map<String, String> variables = template.match(uri);
            String shopIdStr = variables.get("shopId");
            return shopIdStr != null ? Long.parseLong(shopIdStr) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
