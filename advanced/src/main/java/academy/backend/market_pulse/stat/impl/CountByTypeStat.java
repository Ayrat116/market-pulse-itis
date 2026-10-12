package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Количество котировок по типу инструмента ({@code groupingBy} + {@code counting}). */
public class CountByTypeStat implements Stat<Map<InstrumentType, Long>> {

    @Getter
    private final StatCode code = StatCode.COUNT_BY_TYPE;

    @Override
    public Map<InstrumentType, Long> calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        quote -> quote.getInstrument().getType(),
                        Collectors.counting()));
    }
}
