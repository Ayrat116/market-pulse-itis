package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Скользящее среднее цены по окнам из {@value #WINDOW_SIZE} подряд идущих котировок — версия
 * «в лоб», на базовых операциях стрима и коллекторах, без Gatherer API (который появится позже,
 * см. {@link MovingAveragePriceStat} — та же формула, переписанная через кастомный
 * {@code Gatherer}).
 *
 * <p>Среди промежуточных операций стрима нет «окна из последних N элементов» — ближайшее
 * приближение: вручную перебрать начальные индексы окон через {@code IntStream.rangeClosed} и для
 * каждого индекса получить окно через {@code List.subList} (не стрим-операция — обычный метод
 * {@code List}). Дальше для каждого окна — обычный {@code collect(Collectors.averagingDouble(...))}.
 * Работает, но плодит {@code subList}-представления и пересчитывает среднее заново для каждого
 * окна — {@code O(N * windowSize)} на весь список, а не {@code O(N)}, как у `Gatherer`-версии.
 */
public class NaiveMovingAveragePriceStat implements Stat<List<Double>> {

    private static final int WINDOW_SIZE = 3;

    @Getter
    private final StatCode code = StatCode.NAIVE_MOVING_AVERAGE_PRICE;

    @Override
    public List<Double> calculateStats(List<Quote> quotes) {
        List<BigDecimal> prices = quotes.stream().map(Quote::getPrice).toList();
        if (prices.size() < WINDOW_SIZE) {
            return List.of();
        }
        return IntStream.rangeClosed(0, prices.size() - WINDOW_SIZE)
                .mapToObj(start -> prices.subList(start, start + WINDOW_SIZE).stream()
                        .collect(Collectors.averagingDouble(BigDecimal::doubleValue)))
                .toList();
    }
}
