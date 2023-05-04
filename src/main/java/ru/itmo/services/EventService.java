package ru.itmo.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import ru.itmo.model.Action;
import ru.itmo.model.AddedVisit;
import ru.itmo.model.Event;
import ru.itmo.model.FullEvent;
import ru.itmo.model.Ticket;
import ru.itmo.repository.EventRepository;
import ru.itmo.repository.SeasonTicketRepository;
import ru.itmo.services.exceptions.TicketNotFoundException;

import java.util.List;

@Service
public class EventService{
    private final EventRepository eventRepository;
    private final SeasonTicketRepository seasonTicketRepository;
    private final ObjectMapper objectMapper;

    public EventService(
            EventRepository eventRepository,
            SeasonTicketRepository seasonTicketRepository,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.seasonTicketRepository = seasonTicketRepository;
        this.objectMapper = objectMapper;
    }

    public void appendAction(Action action) {
        int currentVersion = seasonTicketRepository
                .getNextVersionBySeasonTicketId(action.sessionTicketId());

        eventRepository.appendEvent(
                new Event(
                        action.sessionTicketId(),
                        currentVersion,
                        action.type(),
                        action.data()
                        )
                );
    }

    public Ticket getSessionTicketById(int id) throws TicketNotFoundException {
        List<FullEvent> sortedEvents = eventRepository.eventBySeasonTicketId(id);

        if (sortedEvents.isEmpty()) {
            throw new TicketNotFoundException("not found ticket with id: " + id);
        }

        int res = 0;
        String name = sortedEvents.get(0).name();

        for (FullEvent fullEvent : sortedEvents) {
            switch (fullEvent.eventType()) {
                case EXTEND -> {
                    try {
                        res += objectMapper.readValue(fullEvent.data(), AddedVisit.class).count();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("unlucky", e);
                    }
                }
                case START_VISIT -> res--;
                case CANCEL -> {
                    return new Ticket(name, 0);
                }
                default -> {}
            }
        }

        return new Ticket(name, res);
    }
}
