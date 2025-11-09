package org.acme.domain;

import com.fasterxml.jackson.databind.JsonNode;

// This class represents a single block in the flow
public class Block {
    public String id;
    public String type; // "MESSAGE" or "INTENT_DETECTION"
    public JsonNode data;
    public String nextBlockId;
}