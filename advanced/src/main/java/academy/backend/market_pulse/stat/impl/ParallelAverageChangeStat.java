package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.spliterator.ArraySpliterator;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.util.List;
import java.util.stream.StreamSupport;

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
        return StreamSupport.stream(new ArraySpliterator<>(quotes), true)
                .mapToDouble(quote -> quote.getChangePercent().doubleValue())
                .average()
                .orElse(0.0);
    }
}
