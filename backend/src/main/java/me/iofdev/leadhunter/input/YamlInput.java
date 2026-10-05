package me.iofdev.leadhunter.input;

import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

public final class YamlInput {

    private static final YAMLMapper YAML = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private YamlInput() {
    }

    public static <T> T read(String content, Class<T> type, String what) {
        T value;
        try {
            value = YAML.readValue(content, type);
        } catch (JacksonException e) {
            throw new InvalidInputException(what, List.of(e.getOriginalMessage()));
        }
        if (value == null) {
            throw new InvalidInputException(what, List.of("file is empty"));
        }
        return value;
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
