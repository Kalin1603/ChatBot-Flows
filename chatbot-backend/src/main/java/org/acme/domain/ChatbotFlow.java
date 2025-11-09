package org.acme.domain;

import java.util.List;

// This class represents the entire JSON configuration
public class ChatbotFlow {
    public String flowId;
    public String name;
    public String startBlockId;
    public List<Block> blocks;
}