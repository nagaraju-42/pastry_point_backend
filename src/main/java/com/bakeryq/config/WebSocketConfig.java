// package com.bakeryq.config;

// import org.springframework.context.annotation.Configuration;
// import org.springframework.messaging.simp.config.MessageBrokerRegistry;
// import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
// import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
// import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// @Configuration
// @EnableWebSocketMessageBroker
// public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//     @Override
//     public void configureMessageBroker(MessageBrokerRegistry registry) {
//         // Prefix for messages FROM server TO client
//         registry.enableSimpleBroker("/topic", "/queue");
//         // Prefix for messages FROM client TO server
//         registry.setApplicationDestinationPrefixes("/app");
//     }

//     @Override
//     public void registerStompEndpoints(StompEndpointRegistry registry) {
//         // WebSocket endpoint — frontend connects to this URL
//         registry.addEndpoint("/ws")
//                 .setAllowedOriginPatterns("*")
//                 .withSockJS();   // SockJS fallback for browsers that don't support WS
//     }
// }
package com.bakeryq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages FROM server TO client
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages FROM client TO server
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint — frontend connects to this URL
        registry.addEndpoint("/ws")
                .setAllowedOrigins(frontendUrl) // 👈 Specific allowed origin for security
                .withSockJS(); 
    }
}