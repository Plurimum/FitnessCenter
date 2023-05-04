package ru.itmo.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import ru.itmo.model.Event;
import ru.itmo.model.Pair;
import ru.itmo.model.Type;
import ru.itmo.model.User;
import ru.itmo.repository.EventRepository;
import ru.itmo.repository.SeasonTicketRepository;

@Service
public class RegistersService {
    private final ObjectMapper objectMapper;
    private final SeasonTicketRepository seasonTicketRepository;
    private final EventRepository eventRepository;

    public RegistersService(
            ObjectMapper objectMapper,
            SeasonTicketRepository seasonTicketRepository,
            EventRepository eventRepository
    ) {
        this.objectMapper = objectMapper;
        this.seasonTicketRepository = seasonTicketRepository;
        this.eventRepository = eventRepository;
    }

    public int registerUser(User user) {
        try {
            String userSerialize = objectMapper.writeValueAsString(user);
            Pair<Integer, Integer> ticketVersionPair = seasonTicketRepository
                    .registerSeasonTicket(user.name());

            eventRepository.appendEvent(
                    new Event(
                            ticketVersionPair.left(),
                            ticketVersionPair.right(),
                            Type.CREATE,
                            userSerialize
                    )
            );

            return ticketVersionPair.left();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("unlucky", e);
        }
    }
}
