package ru.itmo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.model.Action;
import ru.itmo.services.EventService;

@RestController
public class ActionController {
    private final EventService eventService;

    public ActionController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/v1/action")
    ResponseEntity<Void> onExtend(@RequestBody Action action) {
        eventService.appendAction(action);

        return ResponseEntity.ok().build();
    }
}