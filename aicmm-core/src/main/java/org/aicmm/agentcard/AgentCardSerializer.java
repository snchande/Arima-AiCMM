package org.aicmm.agentcard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serializes and deserializes AgentCard instances to/from JSON.
 */
public class AgentCardSerializer {

    private final ObjectMapper mapper;

    public AgentCardSerializer() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Serialize an AgentCard to a JSON string.
     */
    public String toJson(AgentCard card) throws JsonProcessingException {
        return mapper.writeValueAsString(card);
    }

    /**
     * Deserialize an AgentCard from a JSON string.
     */
    public AgentCard fromJson(String json) throws JsonProcessingException {
        return mapper.readValue(json, AgentCard.class);
    }

    /**
     * Write an AgentCard to a JSON file.
     */
    public void writeToFile(AgentCard card, Path path) throws IOException {
        String json = toJson(card);
        Files.writeString(path, json);
    }

    /**
     * Read an AgentCard from a JSON file.
     */
    public AgentCard readFromFile(Path path) throws IOException {
        String json = Files.readString(path);
        return fromJson(json);
    }
}
