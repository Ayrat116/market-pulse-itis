package academy.backend.market_pulse.config;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.model.Watchlist;
import academy.backend.market_pulse.repository.InMemoryInstrumentRepository;
import academy.backend.market_pulse.repository.InstrumentRepository;
import academy.backend.market_pulse.service.QuoteService;
import academy.backend.market_pulse.service.QuoteSource;
import academy.backend.market_pulse.stat.Stat;
import academy.backend.market_pulse.stat.impl.AverageChangeStat;
import academy.backend.market_pulse.stat.impl.CountByCurrencyStat;
import academy.backend.market_pulse.stat.impl.GainLossSplitStat;
import academy.backend.market_pulse.stat.impl.MovingAveragePriceStat;
import academy.backend.market_pulse.stat.impl.NaiveMovingAveragePriceStat;
import academy.backend.market_pulse.stat.impl.ParallelAverageChangeStat;
import academy.backend.market_pulse.stat.impl.TickersByTypeStat;
import academy.backend.market_pulse.stat.impl.TopGainerStat;
import academy.backend.market_pulse.stat.impl.TotalPriceStat;
import academy.backend.market_pulse.stat.impl.WindowedAveragePriceStat;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Композиционный корень приложения: единственное место, где создаются все зависимости, и каждая —
 * ровно один раз (синглтон на время работы процесса). {@link academy.backend.market_pulse.cli.CommandLineFactory}
 * берёт готовые объекты отсюда, а не создаёт их сам.
 */
@Getter
public class AppConfig {

    private final InstrumentRepository instrumentRepository;
    private final Watchlist watchlist;
    private final QuoteSource quoteSource;
    private final QuoteService quoteService;
    private final List<Stat<?>> stats;
    private final Map<String, List<BigDecimal>> priceHistory;

    public AppConfig() {
        this.instrumentRepository = new InMemoryInstrumentRepository();
        this.watchlist = new Watchlist();
        this.quoteSource = new QuoteSource(sampleQuotes());
        this.quoteService = new QuoteService(quoteSource);
        this.stats = stats();
        this.priceHistory = samplePriceHistory();
    }

    /**
     * Заглушка внешнего источника котировок — заранее заготовленные данные вместо реального
     * сетевого запроса. Инструменты здесь — отдельные объекты, не из {@code repository}: источник
     * котировок и каталог инструментов — независимые системы, как и в реальности.
     */
    private static Map<String, Quote> sampleQuotes() {
        Instrument sber = InstrumentFactories.create(InstrumentType.STOCK, "SBER", "Сбербанк", Currency.RUB);
        Instrument gazp = InstrumentFactories.create(InstrumentType.STOCK, "GAZP", "Газпром", Currency.RUB);
        Instrument lkoh = InstrumentFactories.create(InstrumentType.STOCK, "LKOH", "Лукойл", Currency.RUB);
        Instrument aapl = InstrumentFactories.create(InstrumentType.STOCK, "AAPL", "Apple", Currency.USD);
        Instrument ofz = InstrumentFactories.create(InstrumentType.BOND, "OFZ26233", "ОФЗ 26233", Currency.RUB);
        Instrument fxus = InstrumentFactories.create(InstrumentType.ETF, "FXUS", "FinEx FXUS", Currency.USD);

        Map<String, Quote> quotes = new LinkedHashMap<>();
        quotes.put("SBER", new Quote(sber, new BigDecimal("250.00"), new BigDecimal("1.20")));
        quotes.put("GAZP", new Quote(gazp, new BigDecimal("120.50"), new BigDecimal("-0.80")));
        quotes.put("LKOH", new Quote(lkoh, new BigDecimal("7100.00"), new BigDecimal("2.30")));
        quotes.put("AAPL", new Quote(aapl, new BigDecimal("225.30"), new BigDecimal("0.50")));
        quotes.put("OFZ26233", new Quote(ofz, new BigDecimal("780.00"), new BigDecimal("-0.15")));
        quotes.put("FXUS", new Quote(fxus, new BigDecimal("6.85"), new BigDecimal("0.90")));
        return quotes;
    }

    /**
     * Статистики, которые печатает {@code stats}. Добавить новую — написать класс, реализующий
     * {@link Stat}, в {@code stat.impl}, и дописать его сюда; сам {@code StatsCommand} не
     * меняется — ровно тот же принцип, что у {@code InstrumentFilter}.
     *
     * <p>Список сгруппирован по технике вычисления: сначала базовые статистики на дефолтных
     * операциях стрима ({@code NaiveMovingAveragePriceStat} — версия «в лоб» без Gatherer API),
     * затем {@code ParallelAverageChangeStat} — многопоточные стримы на кастомном
     * {@code Spliterator} ({@code spliterator/ArraySpliterator.java}), и наконец Gatherer API:
     * {@code WindowedAveragePriceStat} — встроенный ({@code Gatherers.windowSliding}),
     * {@code MovingAveragePriceStat} — кастомный ({@code gather/MovingAverageGatherer.java}),
     * переписывает {@code NaiveMovingAveragePriceStat} той же формулой, другим способом.
     */
    private static List<Stat<?>> stats() {
        return List.of(
                new TickersByTypeStat(),
                new CountByCurrencyStat(),
                new AverageChangeStat(),
                new TotalPriceStat(),
                new GainLossSplitStat(),
                new TopGainerStat(),
                new NaiveMovingAveragePriceStat(),
                new ParallelAverageChangeStat(),
                new WindowedAveragePriceStat(),
                new MovingAveragePriceStat());
    }

    /**
     * Заглушка истории цен по тикерам — для команды {@code history} (скользящее среднее через
     * Gatherer API). Отдельная {@code Map}, не часть {@code QuoteSource}: истории нет у
     * реального источника.
     */
    private static Map<String, List<BigDecimal>> samplePriceHistory() {
        Map<String, List<BigDecimal>> history = new LinkedHashMap<>();
        history.put("SBER", List.of(
                new BigDecimal("244.00"), new BigDecimal("246.50"), new BigDecimal("245.00"),
                new BigDecimal("248.00"), new BigDecimal("250.00"), new BigDecimal("249.50"),
                new BigDecimal("251.20")));
        history.put("LKOH", List.of(
                new BigDecimal("6950.00"), new BigDecimal("7020.00"), new BigDecimal("6990.00"),
                new BigDecimal("7080.00"), new BigDecimal("7100.00"), new BigDecimal("7150.00")));
        return history;
    }
}
