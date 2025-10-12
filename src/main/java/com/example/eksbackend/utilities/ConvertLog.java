package com.example.eksbackend.utilities;

import com.example.eksbackend.model.LogFalco;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConvertLog {


    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static LogFalco mapTopEntityLog(String log) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(log);

        LogFalco event = new LogFalco();
        event.setHostname(root.path("hostname").asText());
        event.setOutput(root.path("output").asText());
        event.setPriority(root.path("priority").asText());
        event.setRule(root.path("rule").asText());
        event.setSource(root.path("source").asText());

        String timeStr = root.path("time").asText();
        Date parsedDate = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            parsedDate = sdf.parse(timeStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        event.setTime(parsedDate);

        List<String> tags = new ArrayList<>();
        if (root.has("tags") && root.get("tags").isArray()) {
            for (JsonNode tag : root.get("tags")) {
                tags.add(tag.asText());
            }
        }
        event.setTags(tags);

        Map<String, String> outputFields = new HashMap<>();
        if (root.has("output_fields")) {
            JsonNode fields = root.get("output_fields");
            fields.fieldNames().forEachRemaining(key -> {
                String normalizedKey = key.replace(".", "_");
                outputFields.put(normalizedKey, fields.get(key).asText(""));
            });
        }
        event.setOutputFields(outputFields);




        return event;
    }
}
