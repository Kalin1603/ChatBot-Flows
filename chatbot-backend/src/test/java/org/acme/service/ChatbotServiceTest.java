package org.acme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import org.acme.domain.Block;
import org.acme.domain.ChatbotFlow;
import org.acme.persistence.ConversationEntry;
import org.acme.service.gemini.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@QuarkusTest
class ChatbotServiceTest {

    @Inject
    ChatbotService chatbotService;

    @Inject
    ConfigService configService;

    @InjectMock
    GeminiService geminiServiceMock;

    @BeforeEach
    @Transactional
    void setUp() {
        ConversationEntry.deleteAll();

        // Create a simple flow for testing
        ObjectMapper mapper = new ObjectMapper();
        ChatbotFlow flow = new ChatbotFlow();
        flow.flowId = "test-flow";
        flow.startBlockId = "block-welcome";

        Block blockWelcome = new Block();
        blockWelcome.id = "block-welcome";
        blockWelcome.type = "MESSAGE";
        blockWelcome.data = mapper.createObjectNode().put("text", "Welcome!");
        blockWelcome.nextBlockId = "block-intent";

        Block blockIntent = new Block();
        blockIntent.id = "block-intent";
        blockIntent.type = "INTENT_DETECTION";
        ObjectNode data1 = mapper.createObjectNode();
        data1.putArray("intents").add("Get Weather");
        data1.set("mappings", mapper.createObjectNode().put("Get Weather", "block-weather"));
        data1.put("fallbackBlockId", "block-fallback");
        blockIntent.data = data1;

        Block blockWeather = new Block();
        blockWeather.id = "block-weather";
        blockWeather.type = "MESSAGE";
        blockWeather.data = mapper.createObjectNode().put("text", "It is sunny.");
        blockWeather.nextBlockId = null;

        // A fallback block for completeness
        Block blockFallback = new Block();
        blockFallback.id = "block-fallback";
        blockFallback.type = "MESSAGE";
        blockFallback.data = mapper.createObjectNode().put("text", "I do not understand.");
        blockFallback.nextBlockId = null;

        flow.blocks = List.of(blockWelcome, blockIntent, blockWeather, blockFallback);
        configService.updateFlow(flow);
    }

    @Test
    @Transactional
    void testFullConversationFlow() throws InterruptedException {
        // Arrange
        Session sessionMock = Mockito.mock(Session.class);
        RemoteEndpoint.Async asyncRemoteMock = Mockito.mock(RemoteEndpoint.Async.class);
        Mockito.when(sessionMock.getId()).thenReturn("session-123");
        Mockito.when(sessionMock.getAsyncRemote()).thenReturn(asyncRemoteMock);
        doAnswer(invocation -> {
            SendHandler handler = invocation.getArgument(1);
            handler.onResult(new SendResult());
            return null;
        }).when(asyncRemoteMock).sendText(any(String.class), any(SendHandler.class));

        String userMessage = "what is the weather like today?";
        Mockito.when(geminiServiceMock.determineIntent(Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Uni.createFrom().item("Get Weather"));

        // Act

        // Simulate user connecting. This gets the service into the "waiting" state.
        chatbotService.handleNewConnection(sessionMock);

        // A small wait to ensure the welcome message persistence is complete
        Thread.sleep(200);

        // Simulate the user sending message.
        chatbotService.handleUserMessage(sessionMock, userMessage);

        // A longer wait for the Gemini call and the DB writes
        Thread.sleep(500);

        // Assert

        // Verifying all three messages were saved correctly
        assertEquals(3, ConversationEntry.count());

        ConversationEntry welcomeMsg = ConversationEntry.find("actor = 'BOT' and message = 'Welcome!'").firstResult();
        assertEquals("block-welcome", welcomeMsg.blockId);

        ConversationEntry userMsg = ConversationEntry.find("actor = 'USER' and message = ?1", userMessage).firstResult();
        assertEquals("block-intent", userMsg.blockId);

        ConversationEntry responseMsg = ConversationEntry.find("actor = 'BOT' and message = 'It is sunny.'").firstResult();
        assertEquals("block-weather", responseMsg.blockId);
    }
}