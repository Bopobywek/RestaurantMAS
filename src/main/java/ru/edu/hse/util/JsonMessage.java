package ru.edu.hse.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jade.lang.acl.ACLMessage;
import lombok.SneakyThrows;

public class JsonMessage extends ACLMessage {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public JsonMessage(int cfp) {
        super(cfp);
    }

    @SneakyThrows
    public <T> T getContent(Class<T> clazz) {
        return objectMapper.readValue(super.getContent(), clazz);
    }

    @SneakyThrows
    public <T> void setContent(T content) {
        super.setContent(objectMapper.writeValueAsString(content));
    }
}
