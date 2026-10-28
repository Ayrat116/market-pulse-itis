package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Gatherers;

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
        return quotes.stream()
                .map(Quote::getPrice)
                .gather(Gatherers.windowSliding(WINDOW_SIZE))
                .map(window -> window.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(window.size()), 2, RoundingMode.HALF_UP))
                .toList();
    }
}
