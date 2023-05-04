package ru.itmo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Action(
        @JsonProperty(value = "session_ticket_id", required = true) int sessionTicketId,
        @JsonProperty("type") Type type,
        @JsonProperty("data") String data
) {
}
