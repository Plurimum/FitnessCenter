package ru.itmo.model;

public record Event(
        int seasonTicketId,
        int version,
        Type eventType,
        String data
) {
}
