package ru.edu.hse.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jade.lang.acl.ACLMessage;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ACLMessageUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public static <T> T getContent(ACLMessage message, Class<T> clazz) {
        return objectMapper.readValue(message.getContent(), clazz);
    }
}