package org.acme.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.Instant;

@Entity
public class ConversationEntry extends PanacheEntity { // Extending PanacheEntity gives us an automatic 'id' field

    public String conversationId;
    public Instant timestamp;
    public String actor; // "USER" or "BOT"
    public String message;
    public String blockId; // The blockId that was active when this message was sent/received

    public ConversationEntry() {
        // Default constructor required by JPA
    }

    public ConversationEntry(String conversationId, String actor, String message, String blockId) {
        this.conversationId = conversationId;
        this.actor = actor;
        this.message = message;
        this.blockId = blockId;
        this.timestamp = Instant.now();
    }
}