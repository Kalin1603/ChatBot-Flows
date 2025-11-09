package org.acme.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domain.ChatbotFlow;
import org.acme.service.ConfigService;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON) // All methods in this class will return JSON
@Consumes(MediaType.APPLICATION_JSON) // All methods in this class expect JSON
public class ChatbotConfigResource {

    @Inject // This tells Quarkus to automatically inject the ConfigService instance
    ConfigService configService;

    /**
     * Endpoint to upload a new or updated JSON configuration.
     * HTTP Method: POST
     * URL: /api/config
     */
    @POST
    public Response uploadConfig(ChatbotFlow flow) {
        try {
            configService.updateFlow(flow);
            // Return a 200 OK response with a simple success message
            return Response.ok("{\"status\":\"success\"}").build();
        } catch (IllegalArgumentException e) {
            // If the flow is invalid, return a 400 Bad Request
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            // For any other unexpected errors, 500 Internal Server Error
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"An unexpected error occurred.\"}")
                    .build();
        }
    }

    /**
     * Endpoint to get the current JSON configuration.
     * HTTP Method: GET
     * URL: /api/config
     */
    @GET
    public Response getConfig() {
        ChatbotFlow currentFlow = configService.getFlow();
        if (currentFlow != null) {
            // If a flow exists - 200 OK status
            return Response.ok(currentFlow).build();
        } else {
            // If no flow has been configured - 404 Not Found status
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Configuration not found.\"}")
                    .build();
        }
    }
}