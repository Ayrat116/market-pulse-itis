package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Количество котировок по валюте инструмента ({@code groupingBy} + {@code counting}). */
public class CountByCurrencyStat implements Stat<Map<Currency, Long>> {

    @Getter
    private final StatCode code = StatCode.COUNT_BY_CURRENCY;

    @Override
    public Map<Currency, Long> calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        quote -> quote.getInstrument().getCurrency(),
                        Collectors.counting()));
    }
}
