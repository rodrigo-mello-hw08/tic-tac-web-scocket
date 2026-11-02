package com.tic.websocket;

import com.tic.domain.pojo.GameMessages.Event;
import com.tic.exception.GameRuleException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(GameRuleException.class)
    @SendToUser(value = "/queue/users", broadcast = false)
    public Event handleRuleError(GameRuleException exception) {
        return Event.error(exception.getMessage());
    }
}
