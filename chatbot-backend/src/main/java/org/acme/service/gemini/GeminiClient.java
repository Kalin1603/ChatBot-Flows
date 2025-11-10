package org.acme.service.gemini;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "gemini.api")
public interface GeminiClient {

    @POST
    @Path("/v1beta/models/gemini-1.5-flash-latest:generateContent")
    Uni<GeminiResponse> generateContent(
            @HeaderParam("x-goog-api-key") String apiKey, 
            GeminiRequest request
    );
}