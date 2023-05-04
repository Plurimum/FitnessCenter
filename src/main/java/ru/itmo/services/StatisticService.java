package ru.itmo.services;

import org.springframework.stereotype.Service;
import ru.itmo.model.FromTo;
import ru.itmo.model.FullEvent;
import ru.itmo.model.Statistic;
import ru.itmo.repository.EventRepository;
import ru.itmo.repository.SeasonTicketRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Service
public class StatisticService {
    private final EventRepository eventRepository;
    private final SeasonTicketRepository seasonTicketRepository;

    public StatisticService(EventRepository eventRepository, SeasonTicketRepository seasonTicketRepository) {
        this.eventRepository = eventRepository;
        this.seasonTicketRepository = seasonTicketRepository;
    }

    public Statistic getStatistic() {
        Stream<FullEvent> sortedEvents = seasonTicketRepository.findAll().stream()
                .flatMap(id -> eventRepository.eventBySeasonTicketId(id).stream());

        return getStatistic(sortedEvents);
    }

    public Statistic getStatistic(int id) {
        Stream<FullEvent> sortedEvents = eventRepository.eventBySeasonTicketId(id).stream();

        return getStatistic(sortedEvents);
    }

    private Statistic getStatistic(Stream<FullEvent> eventStream) {
        AtomicInteger countTimes = new AtomicInteger(0);
        AtomicReference<Instant> currentStart = new AtomicReference<>();
        List<Long> intervals = new ArrayList<>();

        eventStream.forEach(event -> {
                    switch (event.eventType()) {
                        case START_VISIT -> {
                            currentStart.getAndSet(event.createdAt());
                            countTimes.incrementAndGet();
                        }
                        case END_VISIT -> intervals.add(
                                event.createdAt().toEpochMilli() -
                                        currentStart.get().toEpochMilli()
                        );
                        default -> {
                        }
                    }
                }
        );

        if (countTimes.get() != intervals.size()) {
            intervals.add(Instant.now().toEpochMilli() - currentStart.get().toEpochMilli());
        }

        long intervalsSum = intervals.stream()
                .reduce(0L, Long::sum);

        return new Statistic(
                Instant.ofEpochMilli(intervalsSum / countTimes.get()),
                countTimes.get()
        );
    }

    public int getCountVisit(FromTo fromTo) {
        return eventRepository.countEvents(fromTo.from(), fromTo.to());
    }
}
