package org.acme.service.gemini;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class GeminiService {

    @RestClient // Injects the REST client
    GeminiClient geminiClient;

    @ConfigProperty(name = "gemini.api.key") // Injects the API key from application.properties
    String apiKey;

    /**
     * Asks the Gemini API to determine the user's intent.
     * @param userMessage The message from the user.
     * @param possibleIntents A list of valid intents from the JSON config.
     * @return A Uni that will eventually contain the string of the matched intent.
     */
    public Uni<String> determineIntent(String userMessage, List<String> possibleIntents) {
        String prompt = buildPrompt(userMessage, possibleIntents);

        // LOGGING: STEP 1
        System.out.println("\n=========================================================");
        System.out.println("[GeminiService] PROMPT SENT TO GEMINI:");
        System.out.println(prompt);
        System.out.println("=========================================================\n");

        Part part = new Part(prompt);
        Content content = new Content(Collections.singletonList(part));
        GeminiRequest request = new GeminiRequest(Collections.singletonList(content));

        return geminiClient.generateContent(apiKey, request)
                .onFailure().invoke(failure -> {
                    // LOGGING: STEP 2 (FAILURE)
                    System.err.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.err.println("[GeminiService] GEMINI API CALL FAILED!");
                    System.err.println(failure.getMessage());
                    System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
                })
                .onItem().transform(response -> {
                    String rawText = response.firstTextResult();

                    // LOGGING: STEP 3 (SUCCESS)
                    System.out.println("\n=========================================================");
                    System.out.println("[GeminiService] RAW RESPONSE FROM GEMINI:");
                    System.out.println("'" + rawText + "'");
                    System.out.println("=========================================================\n");

                    if (rawText == null || rawText.isBlank()) {
                        System.err.println("[GeminiService] Decision: Gemini returned a blank response. Defaulting to NO_MATCH.");
                        return "NO_MATCH";
                    }
                    String cleanedText = rawText.trim().replace("`", "").replace("\"", "");

                    if (possibleIntents.contains(cleanedText)) {
                        System.out.println("[GeminiService] Decision: Matched intent '" + cleanedText + "'.");
                        return cleanedText;
                    } else {
                        System.err.println("[GeminiService] Decision: Gemini response '" + cleanedText + "' is not a valid intent. Defaulting to NO_MATCH.");
                        return "NO_MATCH";
                    }
                })
                .onFailure().recoverWithItem(failure -> {
                    // This is a fallback in case the failure logging itself fails.
                    System.err.println("[GeminiService] Recovering from failure, returning NO_MATCH.");
                    return "NO_MATCH";
                });
    }

    /**
     * Constructs the prompt for the LLM with clear instructions.
     */
    private String buildPrompt(String userMessage, List<String> possibleIntents) {
        // Joining the intents
        String intentsString = possibleIntents.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", "));

        return "You are an expert intent classifier. Your task is to determine which of the predefined intents best matches the user's message." +
                " The user's message is: \"" + userMessage + "\"" +
                " The possible intents are: [" + intentsString + "]." +
                " Analyze the user's message and respond with ONLY the single, exact string of the best matching intent from the provided list." +
                " Do not add any explanation, punctuation, or other text." +
                " If no intent is a clear match, respond with the exact string \"NO_MATCH\".";
    }
}