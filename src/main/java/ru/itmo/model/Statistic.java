package ru.itmo.model;

import java.time.Instant;

public record Statistic(Instant averageTimeFitness, int countTimes) {
}
