package com.tic.websocket;

import com.tic.domain.pojo.GameMessages;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class ConnectionTestController {

    @MessageMapping("/ping-pong")
    @SendTo("/topic/ping-pong")
    public GameMessages.Event health(GameMessages.HealthRequest request) {
        if (request == null || !"ping".equalsIgnoreCase(request.message())) {
            return GameMessages.Event.error("Expected message 'ping'");
        }
        return GameMessages.Event.success("PONG", java.util.Map.of("message", "pong"));
    }

}
