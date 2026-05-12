package com.motorcycle.repair.config;

import com.motorcycle.repair.websocket.ShopWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ShopWebSocketHandler shopWebSocketHandler;

    public WebSocketConfig(ShopWebSocketHandler shopWebSocketHandler) {
        this.shopWebSocketHandler = shopWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(shopWebSocketHandler, "/ws/shop/{shopId}")
                .setAllowedOrigins("*");
    }
}
