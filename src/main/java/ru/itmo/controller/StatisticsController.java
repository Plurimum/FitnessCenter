package ru.itmo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.model.FromTo;
import ru.itmo.model.Statistic;
import ru.itmo.services.StatisticService;

@RestController
public class StatisticsController {
    private final StatisticService statisticService;

    public StatisticsController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    @PostMapping("/v1/statistic/visits/{id}")
    public ResponseEntity<Statistic> onStatisticVisits(
            @PathVariable(name = "id", required = false) int id
    ) {
        return ResponseEntity.ok(statisticService.getStatistic(id));
    }

    @PostMapping("/v1/count/visits")
    public ResponseEntity<Integer> onCountVisits(@RequestBody FromTo fromTo) {
        return ResponseEntity.ok(statisticService.getCountVisit(fromTo));
    }
}
