package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.spliterator.ArraySpliterator;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.util.List;
import java.util.stream.StreamSupport;

// NOTICE: семинарист пишет этот класс вживую — демонстрация многопоточных стримов на
// собственном Spliterator'е (см. ArraySpliterator), а не на встроенном parallelStream().
/**
 * Та же средняя доходность, что {@link AverageChangeStat}, но стрим собирается вручную из
 * самописного {@link ArraySpliterator} через {@code StreamSupport.stream(spliterator, true)}, а не
 * из {@code quotes.parallelStream()} — показывает, что параллельный стрим можно построить над
 * произвольным {@code Spliterator}, не только над {@code Collection}. Результат совпадает с
 * {@link AverageChangeStat} — разница только в источнике деления работы между потоками
 * {@code ForkJoinPool}.
 */
public class ParallelAverageChangeStat implements Stat<Double> {

    @Getter
    private final StatCode code = StatCode.PARALLEL_AVERAGE_CHANGE;

    @Override
    public Double calculateStats(List<Quote> quotes) {
        // NOTICE: StreamSupport.stream(new ArraySpliterator<>(quotes), true) — параллельный стрим
        // строится вручную над своим Spliterator'ом, а не через quotes.parallelStream(). Формула —
        // та же, что в AverageChangeStat: результат должен совпасть, разница только в источнике
        // деления работы между потоками ForkJoinPool.
        // TODO: семинар 6, этап 4 — StreamSupport.stream(spliterator, true) + mapToDouble + average
        throw new UnsupportedOperationException("ParallelAverageChangeStat ещё не реализован");
    }
}
