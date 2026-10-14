package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Для каждого типа инструмента — строка со всеми его тикерами через {@code ", "}. Составной
 * {@code collect}: {@code groupingBy(classifier, downstream)}, у которого downstream сам составной
 * — {@code mapping(valueExtractor, joining(delimiter))}. {@code mapping} сначала проецирует элемент
 * группы (котировку) в тикер, и только потом {@code joining} склеивает тикеры внутри группы; без
 * {@code mapping} пришлось бы отдать {@code joining} прямо на {@code Quote}, а {@code Quote} — не
 * {@code CharSequence}, так не скомпилируется.
 */
public class TickersByTypeStat implements Stat<Map<InstrumentType, String>> {

    @Getter
    private final StatCode code = StatCode.TICKERS_BY_TYPE;

    @Override
    public Map<InstrumentType, String> calculateStats(List<Quote> quotes) {
        // NOTICE: groupingBy(classifier, downstream), downstream сам составной —
        // mapping(valueExtractor, joining(delimiter)). mapping сначала проецирует элемент группы
        // (котировку) в тикер, и только потом joining склеивает тикеры внутри группы — без
        // mapping joining пришлось бы отдать прямо на Quote, а Quote не CharSequence.
        // TODO: groupingBy + mapping + joining
        throw new UnsupportedOperationException("TickersByTypeStat ещё не реализован");
    }
}
