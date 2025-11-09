package org.acme.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.acme.service.ChatbotService;

@ServerEndpoint("/chatbot") // Defines the WebSocket URL
@ApplicationScoped
public class StartWebSocket {

    @Inject
    ChatbotService chatbotService; // Inject our business logic service

    @OnOpen
    public void onOpen(Session session) {
        // When a new user connects, delegate to the service
        chatbotService.handleNewConnection(session);
    }

    @OnClose
    public void onClose(Session session) {
        // When a user disconnects, delegate to the service
        chatbotService.handleConnectionClose(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // When a message is received from a user, delegate to the service
        chatbotService.handleUserMessage(session, message);
    }

    // An @OnError method to handle any communication errors
    public void onError(Session session, Throwable throwable) {
         System.err.println("WebSocket error for session " + session.getId() + ": " + throwable.getMessage());
    }
}