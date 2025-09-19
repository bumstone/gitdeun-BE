package com.teamEWSN.gitdeun.common.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeParseException;

public class IsoToLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String s = p.getText();
        if (s == null || s.isBlank()) return null;

        // 1) 오프셋/Z 포함 → UTC LDT로
        try {
            return OffsetDateTime.parse(s).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        // 2) Instant 순수 파싱 시도
        try {
            return Instant.parse(s).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        // 3) 오프셋 없는 LocalDateTime 포맷(레거시) 방어
        return LocalDateTime.parse(s);
    }
}
