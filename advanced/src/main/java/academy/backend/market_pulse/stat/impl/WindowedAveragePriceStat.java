package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Gatherers;

// NOTICE: семинарист пишет этот класс вживую — встроенный Gatherer (Gatherers.windowSliding),
// в паре со следующим (MovingAveragePriceStat — тот же результат через кастомный Gatherer).
/**
 * Среднее цены по окнам из {@value #WINDOW_SIZE} подряд идущих котировок (в порядке списка) —
 * встроенный {@code Gatherers.windowSliding}, который сам собирает окна-списки; дальше остаётся
 * только посчитать среднее в каждом {@code map}'ом. Сравнить с {@link MovingAveragePriceStat} —
 * тот же результат через кастомный {@code Gatherer}, без материализации списков-окон.
 */
public class WindowedAveragePriceStat implements Stat<List<BigDecimal>> {

    private static final int WINDOW_SIZE = 3;

    @Getter
    private final StatCode code = StatCode.WINDOWED_AVERAGE_PRICE;

    @Override
    public List<BigDecimal> calculateStats(List<Quote> quotes) {
        // NOTICE: Gatherers.windowSliding(WINDOW_SIZE) сам собирает окна-списки — дальше обычный
        // map считает среднее в каждом (reduce + divide, HALF_UP до 2 знаков). Сравнить с
        // NaiveMovingAveragePriceStat (этап 3, то же без Gatherer) и с MovingAveragePriceStat
        // (этап 5, кастомный Gatherer без материализации списков-окон).
        // TODO: семинар 6, этап 5 — Gatherers.windowSliding + map
        throw new UnsupportedOperationException("WindowedAveragePriceStat ещё не реализован");
    }
}
