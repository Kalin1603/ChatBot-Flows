package org.acme.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.Session;
import org.acme.domain.Block;
import org.acme.domain.ChatbotFlow;
import org.acme.persistence.ConversationEntry;
import org.acme.service.gemini.GeminiService;

import java.time.Duration; // <-- NEW IMPORT
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatbotService {

    @Inject
    ConfigService configService;

    @Inject
    GeminiService geminiService;

    private final Map<String, String> userStates = new ConcurrentHashMap<>();

    @Transactional
    public void handleNewConnection(Session session) {
        ChatbotFlow flow = configService.getFlow();
        if (flow == null) {
            sendMessage(session, "Chatbot not configured. Please upload a flow.", null);
            return;
        }
        System.out.println("New connection: " + session.getId() + ". Starting flow.");
        processBlock(session, flow.startBlockId);
    }

    public void handleConnectionClose(Session session) {
        userStates.remove(session.getId());
        System.out.println("Connection closed: " + session.getId());
    }

    @Transactional
    public void handleUserMessage(Session session, String userMessage) {
        ChatbotFlow flow = configService.getFlow();
        String currentBlockId = userStates.get(session.getId());

        ConversationEntry userEntry = new ConversationEntry(session.getId(), "USER", userMessage, currentBlockId);
        userEntry.persist();

        if (currentBlockId == null) {
            sendMessage(session, "Error: No current state found for your session. Restarting.", null);
            handleNewConnection(session);
            return;
        }

        Optional<Block> currentBlockOpt = findBlockById(flow, currentBlockId);
        if (currentBlockOpt.isEmpty() || !"INTENT_DETECTION".equals(currentBlockOpt.get().type)) {
            sendMessage(session, "Error: I was not expecting a message right now.", currentBlockId);
            return;
        }

        Block currentBlock = currentBlockOpt.get();
        List<String> possibleIntents = getIntentsFromBlock(currentBlock);

        // Call our Gemini service.
        String matchedIntent = geminiService.determineIntent(userMessage, possibleIntents)
                .await().atMost(Duration.ofSeconds(15)); // Block for up to 15 seconds.

        System.out.println("User '" + session.getId() + "' said '" + userMessage + "'. Gemini detected intent: " + matchedIntent);
        String nextBlockId = mapIntentToBlockId(currentBlock, matchedIntent);
        processBlock(session, nextBlockId);
    }

    // This is the core logic engine. It processes a block and decides what to do next.
    @Transactional
    public void processBlock(Session session, String blockId) {
        if (blockId == null) {
            System.out.println("Flow ended for session: " + session.getId());
            return;
        }

        ChatbotFlow flow = configService.getFlow();
        Optional<Block> blockOpt = findBlockById(flow, blockId);

        if (blockOpt.isEmpty()) {
            sendMessage(session, "Error: Flow is corrupted. Cannot find block with ID: " + blockId, blockId);
            return;
        }

        Block block = blockOpt.get();
        switch (block.type) {
            case "MESSAGE":
                String message = block.data.get("text").asText();
                sendMessage(session, message, block.id);
                processBlock(session, block.nextBlockId);
                break;

            case "INTENT_DETECTION":
                userStates.put(session.getId(), block.id);
                System.out.println("Waiting for user input at block '" + block.id + "' for session: " + session.getId());
                break;

            default:
                sendMessage(session, "Error: Unknown block type '" + block.type + "'.", block.id);
                break;
        }
    }

    // UTILITY METHODS

    private Optional<Block> findBlockById(ChatbotFlow flow, String blockId) {
        return flow.blocks.stream().filter(b -> b.id.equals(blockId)).findFirst();
    }

    private List<String> getIntentsFromBlock(Block block) {
        List<String> intents = new ArrayList<>();
        JsonNode intentsNode = block.data.get("intents");
        if (intentsNode != null && intentsNode.isArray()) {
            for (JsonNode node : intentsNode) {
                intents.add(node.asText());
            }
        }
        return intents;
    }

    private String mapIntentToBlockId(Block block, String intent) {
        JsonNode mappingNode = block.data.get("mappings").get(intent);
        if (mappingNode != null) {
            return mappingNode.asText();
        }
        return block.data.get("fallbackBlockId").asText();
    }

    private void sendMessage(Session session, String text, String currentBlockId) {
        ConversationEntry botEntry = new ConversationEntry(session.getId(), "BOT", text, currentBlockId);
        botEntry.persist();
        // Using getAsyncRemote() for non-blocking sends, which is required on an I/O thread.
        session.getAsyncRemote().sendText(text, result -> {
            if (!result.isOK()) {
                System.err.println("Error sending message to " + session.getId() + ": " + result.getException());
            }
        });
    }
}