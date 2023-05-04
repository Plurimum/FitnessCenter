package ru.itmo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.model.Action;
import ru.itmo.model.Ticket;
import ru.itmo.model.Type;
import ru.itmo.model.User;
import ru.itmo.services.EventService;
import ru.itmo.services.RegistersService;
import ru.itmo.services.exceptions.TicketNotFoundException;

@RestController
public class SessionTicketController {
    private final RegistersService registersService;
    private final EventService eventService;

    public SessionTicketController(RegistersService registersService, EventService eventService) {
        this.registersService = registersService;
        this.eventService = eventService;
    }

    @PostMapping("/v1/register")
    public ResponseEntity<Integer> onRegister(@RequestBody User user) {
        return ResponseEntity.ok(registersService.registerUser(user));
    }

    @PostMapping("/v1/get/session-ticket")
    public ResponseEntity<Ticket> onGetCountVisits(@RequestBody int id) {
        try {
            return ResponseEntity.ok(eventService.getSessionTicketById(id));
        } catch (TicketNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/v1/in")
    public ResponseEntity<Boolean> onIn(@RequestBody int id) {
        try {
            Ticket ticket = eventService.getSessionTicketById(id);

            if (ticket.count() > 0) {
                eventService.appendAction(
                        new Action(
                                id,
                                Type.START_VISIT,
                                "{}"
                        )
                );

                return ResponseEntity.ok(true);
            } else {
                return ResponseEntity.ok(false);
            }
        } catch (TicketNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
