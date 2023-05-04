package ru.itmo.model;

import java.time.Instant;

public record FullEvent(
        String name,
        int seasonTicketId,
        int version,
        Instant createdAt,
        Type eventType,
        String data
) {
}
