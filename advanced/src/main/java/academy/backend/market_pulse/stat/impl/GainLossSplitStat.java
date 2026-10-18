package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сколько котировок выросло ({@code changePercent > 0}), а сколько — нет: ровно два ключа,
 * {@code true}/{@code false}. {@code partitioningBy} со вторым коллектором ({@code counting}) —
 * как {@code groupingBy} в {@link CountByTypeStat}, но классификатор здесь булев, а не
 * произвольный, поэтому и коллектор подходит более специализированный.
 */
public class GainLossSplitStat implements Stat<Map<Boolean, Long>> {

    @Getter
    private final StatCode code = StatCode.GAIN_LOSS_SPLIT;

    @Override
    public Map<Boolean, Long> calculateStats(List<Quote> quotes) {
        return quotes.stream().collect(Collectors.partitioningBy(
                quote -> quote.getChangePercent().signum() > 0,
                Collectors.counting()));
    }
}
