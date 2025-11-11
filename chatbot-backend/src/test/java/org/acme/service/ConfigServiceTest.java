package org.acme.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.domain.ChatbotFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest // This tells Quarkus to start the application for this test
class ConfigServiceTest {

    @Inject
    ConfigService configService;

    // Resetting the state before each test in this class
    @BeforeEach
    void setUp() {
        configService.clear();
    }

    // Simple ChatbotFlow object for testing
    private ChatbotFlow createTestFlow() {
        ChatbotFlow flow = new ChatbotFlow();
        flow.flowId = "test-flow-1";
        flow.name = "Test Flow";
        return flow;
    }

    @Test
    void testUpdateAndGetFlow() {
        // Arrange
        ChatbotFlow testFlow = createTestFlow();
        assertNull(configService.getFlow(), "Initially, the flow should be null.");

        // Act
        configService.updateFlow(testFlow);

        // Assert
        ChatbotFlow retrievedFlow = configService.getFlow();
        assertNotNull(retrievedFlow, "After updating, the flow should not be null.");
        assertEquals("test-flow-1", retrievedFlow.flowId, "The flowId should match the one we set.");
    }

    @Test
    void testUpdateWithNullFlow() {
        // Prove that the validation works
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            configService.updateFlow(null);
        });

        assertEquals("Chatbot flow cannot be null and must have a flowId.", exception.getMessage());
    }
}