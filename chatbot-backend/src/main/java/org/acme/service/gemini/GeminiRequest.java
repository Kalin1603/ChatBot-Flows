package org.acme.service.gemini;

import java.util.List;

public class GeminiRequest {
    public List<Content> contents;

    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
    }
}