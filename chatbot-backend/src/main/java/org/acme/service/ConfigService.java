package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domain.ChatbotFlow;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped //This tells Quarkus to create only one instance of this class
public class ConfigService {

    // Using AtomicReference to hold the configuration for thread-safe updates
    private final AtomicReference<ChatbotFlow> currentFlow = new AtomicReference<>();

    /**
     * Updates the current chatbot flow configuration.
     */
    public void updateFlow(ChatbotFlow newFlow) {
        // Ensure the new flow is not null
        if (newFlow == null || newFlow.flowId == null) {
            throw new IllegalArgumentException("Chatbot flow cannot be null and must have a flowId.");
        }
        this.currentFlow.set(newFlow);
        System.out.println("Chatbot flow updated successfully. New flow ID: " + newFlow.flowId);
    }

    /**
     * Retrieves the current chatbot flow configuration.
     */
    public ChatbotFlow getFlow() {
        return this.currentFlow.get();
    }

    /**
     * Clears the current flow. Intended for use in tests to ensure isolation.
     */
    public void clear() {
        this.currentFlow.set(null);
    }
}