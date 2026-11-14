package br.com.ofisy.notifications.shared.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Reads the trace context that the dd-java-agent injects into the _datadog message attribute.
 * The agent does not continue that trace in the @SqsListener, so the listener restores it with the TraceContextPropagator.
 */
@Component
public class MessageTraceContextReader {
    public static final String TRACE_CONTEXT_ATTRIBUTE = "_datadog";

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTraceContextReader.class);
    private static final TypeReference<Map<String, String>> TRACE_CONTEXT_TYPE = new TypeReference<>() {};

    private final JsonMapper jsonMapper;

    public MessageTraceContextReader(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * The agent publishes the attribute as Binary, which Spring Cloud AWS delivers as SdkBytes; text and bytes are accepted too.
     * A missing or unreadable attribute never blocks consumption: the event is processed without the link to its origin trace.
     */
    public Map<String, String> read(Object attributeValue) {
        String json = asString(attributeValue);
        if (json == null || json.isBlank()) {
            return Map.of();
        }

        try {
            return jsonMapper.readValue(json, TRACE_CONTEXT_TYPE);
        } catch (JacksonException failure) {
            LOGGER.warn("Ignoring unreadable {} message attribute", TRACE_CONTEXT_ATTRIBUTE, failure);
            return Map.of();
        }
    }

    private String asString(Object attributeValue) {
        return switch (attributeValue) {
            case null -> null;
            case SdkBytes sdkBytes -> sdkBytes.asUtf8String();
            case String text -> text;
            case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
            case ByteBuffer buffer -> StandardCharsets.UTF_8.decode(buffer.duplicate()).toString();
            default -> {
                LOGGER.warn("Ignoring {} message attribute of type {}", TRACE_CONTEXT_ATTRIBUTE,
                        attributeValue.getClass().getName());
                yield null;
            }
        };
    }
}
