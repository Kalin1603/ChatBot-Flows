package org.acme.service.gemini;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "gemini-api")
public interface GeminiClient {

    @POST
    @Path("/v1beta/models/gemini-2.5-flash:generateContent")
    Uni<GeminiResponse> generateContent(
            @QueryParam("key") String apiKey,
            GeminiRequest request
    );
}