package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.dictionary.PriceOperator;
import academy.backend.market_pulse.dictionary.sort.SortField;
import academy.backend.market_pulse.dictionary.sort.SortOrder;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.repository.InstrumentRepository;
import academy.backend.market_pulse.util.InstrumentSortingUtils;
import academy.backend.market_pulse.util.ListFilterBuilder;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

@Command(name = "list", description = "Список инструментов")
@RequiredArgsConstructor
public class ListCommand implements Callable<Integer> {

    @Option(names = "--type", description = "Фильтр по типу инструмента")
    private InstrumentType type;

    @Option(names = "--ticker", description = "Фильтр по подстроке в тикере")
    private String ticker;

    @Option(names = "--currency", description = "Фильтр по валюте инструмента")
    private Currency currency;

    @Option(names = "--price-op", description = "Оператор сравнения цены: GE, LE или EQ")
    private PriceOperator priceOperator;

    @Option(names = "--price", description = "Пороговое значение цены (дивидендная доходность акции)")
    private BigDecimal price;

    @Option(names = "--sort-by", description = "Поле сортировки вывода: TICKER, CURRENCY или PRICE")
    private SortField sortBy;

    @Option(names = "--order", description = "Порядок сортировки: ASC (по умолчанию) или DESC")
    private SortOrder order = SortOrder.ASC;

    private final InstrumentRepository repository;

    @Override
    public Integer call() {
        Predicate<Instrument> filter = new ListFilterBuilder()
                .byType(type)
                .byTicker(ticker)
                .byCurrency(currency)
                .byPrice(priceOperator, price)
                .build();

        List<Instrument> matched = new ArrayList<>();
        for (Instrument instrument : repository) {
            if (filter.test(instrument)) {
                matched.add(instrument);
            }
        }

        if (sortBy != null) {
            InstrumentSortingUtils.sortWith(matched, InstrumentSortingUtils.comparator(sortBy, order));
        }

        for (Instrument instrument : matched) {
            System.out.println(instrument.getDescription());
        }
        return 0;
    }
}
