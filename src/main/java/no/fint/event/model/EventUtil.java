package no.fint.event.model;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Utility methods for converting events.
 */
@Slf4j
public enum EventUtil {
    ;

    private static final ObjectMapper objectMapper = JsonMapper.builder().build();

    /**
     * Returns a Event object from a JSON string.
     *
     * @param json JSON string
     * @return {@link Event}
     */
    public static Event toEvent(String json) {
        return toEvent(objectMapper, json);
    }

    public static Event toEvent(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readValue(json, Event.class);
        } catch (JacksonException e) {
            log.warn("Unable to convert json to Event", e);
            return null;
        }
    }

    /**
     * Converts the data field in the Event to a class of <i>type</i>.
     *
     * @param event The Event to convert data from.
     * @param type Type reference
     * @param <T>   Type of data
     * @return A list of objects of the type specified.
     */
    public static <T> List<T> convertEventData(Event event, TypeReference<List<T>> type) {
        return convertEventData(objectMapper, event, type);
    }

    public static <T> List<T> convertEventData(ObjectMapper objectMapper, Event event, TypeReference<List<T>> type) {
        List<?> dataList = event.getData();
        return objectMapper.convertValue(dataList, type);
    }

    public static <T> List<T> convertEventData(Event event, Class<T> type) {
        return convertEventData(objectMapper, event, type);
    }

    public static <T> List<T> convertEventData(ObjectMapper objectMapper, Event event, Class<T> type) {
        List<?> dataList = event.getData();
        return objectMapper.convertValue(dataList, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    /**
     * Returns a JSON string of the Event.
     *
     * @param event Event object
     * @return Event object as a JSON string.
     */
    public static String toJson(Event event) {
        return toJson(objectMapper, event);
    }

    public static String toJson(ObjectMapper objectMapper, Event event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            log.warn("Unable to convert Event to json", e);
            return null;
        }
    }
}
