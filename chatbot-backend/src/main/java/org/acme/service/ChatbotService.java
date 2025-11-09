package org.acme.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.acme.domain.Block;
import org.acme.domain.ChatbotFlow;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatbotService {

    @Inject
    ConfigService configService; // Injecting the service to get the flow configuration

    // Thread-safe map to store the state of each user's conversation.
    // Key: WebSocket Session ID, Value: The ID of the block the user is currently at.
    private final Map<String, String> userStates = new ConcurrentHashMap<>();

    // Called when a new user connects.
    public void handleNewConnection(Session session) {
        ChatbotFlow flow = configService.getFlow();
        if (flow == null) {
            sendMessage(session, "Chatbot not configured. Please upload a flow.");
            return;
        }
        System.out.println("New connection: " + session.getId() + ". Starting flow.");
        // Start the conversation from the beginning of the flow.
        processBlock(session, flow.startBlockId);
    }

    // Called when a user is disconnected.
    public void handleConnectionClose(Session session) {
        // Clean up the user's state when they disconnect.
        userStates.remove(session.getId());
        System.out.println("Connection closed: " + session.getId());
    }

    // Called when a message is received from a user.
    public void handleUserMessage(Session session, String userMessage) {
        ChatbotFlow flow = configService.getFlow();
        String currentBlockId = userStates.get(session.getId());

        if (currentBlockId == null) {
            sendMessage(session, "Error: No current state found for your session. Restarting.");
            handleNewConnection(session);
            return;
        }

        // Find the block the user was waiting at.
        Optional<Block> currentBlockOpt = findBlockById(flow, currentBlockId);
        if (currentBlockOpt.isEmpty() || !"INTENT_DETECTION".equals(currentBlockOpt.get().type)) {
            sendMessage(session, "Error: I was not expecting a message right now.");
            return;
        }

        Block currentBlock = currentBlockOpt.get();

        // Simple text match.
        String nextBlockId = findNextBlockIdForIntent(currentBlock, userMessage);

        System.out.println("User '" + session.getId() + "' said '" + userMessage + "'. Matched intent. Moving to block: " + nextBlockId);
        processBlock(session, nextBlockId);
    }

    // This is the core logic engine. It processes a block and decides what to do next.
    private void processBlock(Session session, String blockId) {
        if (blockId == null) {
            // A null blockId signifies the end of a conversation path.
            System.out.println("Flow ended for session: " + session.getId());
            return;
        }

        ChatbotFlow flow = configService.getFlow();
        Optional<Block> blockOpt = findBlockById(flow, blockId);

        if (blockOpt.isEmpty()) {
            sendMessage(session, "Error: Flow is corrupted. Cannot find block with ID: " + blockId);
            return;
        }

        Block block = blockOpt.get();
        switch (block.type) {
            case "MESSAGE":
                String message = block.data.get("text").asText();
                sendMessage(session, message);
                // Immediately process the next block in the sequence.
                processBlock(session, block.nextBlockId);
                break;

            case "INTENT_DETECTION":
                // Storing the user's state and waiting for their input.
                userStates.put(session.getId(), block.id);
                System.out.println("Waiting for user input at block '" + block.id + "' for session: " + session.getId());
                break;

            default:
                sendMessage(session, "Error: Unknown block type '" + block.type + "'.");
                break;
        }
    }

    // UTILITY METHODS

    private Optional<Block> findBlockById(ChatbotFlow flow, String blockId) {
        return flow.blocks.stream().filter(b -> b.id.equals(blockId)).findFirst();
    }

    // TEMPORARY: Simple intent matching
    private String findNextBlockIdForIntent(Block block, String userMessage) {
        JsonNode intentsNode = block.data.get("intents");
        if (intentsNode != null && intentsNode.isArray()) {
            // Correctly iterate over the elements of the JSON array
            for (final JsonNode intentNode : intentsNode) {
                String intentText = intentNode.asText();
                if (userMessage.equalsIgnoreCase(intentText)) {
                    // If we find a match, get the corresponding next block ID from the mappings.
                    return block.data.get("mappings").get(intentText).asText();
                }
            }
        }
        // If no intent matches, return the fallback block ID.
        return block.data.get("fallbackBlockId").asText();
    }

    private void sendMessage(Session session, String text) {
        // Using getAsyncRemote() for non-blocking sends, which is required on an I/O thread.
        session.getAsyncRemote().sendText(text, result -> {
            if (result.isOK()) {
                System.out.println("Message sent successfully to " + session.getId());
            } else {
                System.err.println("Error sending message to " + session.getId() + ": " + result.getException());
            }
        });
    }
}