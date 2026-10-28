package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Котировка с наибольшим изменением цены — {@code max(Comparator)}, {@code Optional<Quote>}. */
public class TopGainerStat implements Stat<Optional<Quote>> {

    @Getter
    private final StatCode code = StatCode.TOP_GAINER;

    @Override
    public Optional<Quote> calculateStats(List<Quote> quotes) {
        return quotes.stream().max(Comparator.comparing(Quote::getChangePercent));
    }
}
