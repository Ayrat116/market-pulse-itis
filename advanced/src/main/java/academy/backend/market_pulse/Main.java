package academy.backend.market_pulse;

import academy.backend.market_pulse.cli.AddCommand;
import academy.backend.market_pulse.cli.CompareCommand;
import academy.backend.market_pulse.cli.HistoryCommand;
import academy.backend.market_pulse.cli.ListCommand;
import academy.backend.market_pulse.cli.MarketPulseCli;
import academy.backend.market_pulse.cli.MoversCommand;
import academy.backend.market_pulse.cli.QuoteCommand;
import academy.backend.market_pulse.cli.RemoveCommand;
import academy.backend.market_pulse.cli.SaveCommand;
import academy.backend.market_pulse.cli.StatsCommand;
import academy.backend.market_pulse.cli.WatchCommand;
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
import academy.backend.market_pulse.stat.impl.CountByTypeStat;
import academy.backend.market_pulse.stat.impl.GainLossSplitStat;
import academy.backend.market_pulse.stat.impl.MovingAveragePriceStat;
import academy.backend.market_pulse.stat.impl.ParallelAverageChangeStat;
import academy.backend.market_pulse.stat.impl.TopGainerStat;
import academy.backend.market_pulse.stat.impl.TotalPriceStat;
import academy.backend.market_pulse.stat.impl.WindowedAveragePriceStat;
import picocli.CommandLine;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        InstrumentRepository repository = new InMemoryInstrumentRepository();
        Watchlist watchlist = new Watchlist();
        QuoteSource quoteSource = new QuoteSource(sampleQuotes());
        QuoteService quoteService = new QuoteService(quoteSource);

        // NOTICE: подкоманды регистрируются вручную через addSubcommand — у них нет
        //  конструктора без аргументов, каждой нужен уже готовый repository.
        // NOTICE: единой точки обработки непредвиденных исключений сегодня нет — каждая команда
        //  (если вообще) ловит только свои ожидаемые типы. Аналог такой точки в Spring —
        //  @ControllerAdvice/@ExceptionHandler; здесь её пока нет, это открытый вопрос на будущее.
        CommandLine cli = new CommandLine(new MarketPulseCli())
                .addSubcommand(new AddCommand(repository))
                .addSubcommand(new ListCommand(repository))
                .addSubcommand(new SaveCommand(repository))
                .addSubcommand(new WatchCommand(watchlist))
                .addSubcommand(new QuoteCommand(quoteService))
                .addSubcommand(new RemoveCommand(repository, watchlist))
                .addSubcommand(new StatsCommand(quoteSource, stats()))
                .addSubcommand(new CompareCommand(quoteService))
                .addSubcommand(new MoversCommand(quoteSource))
                .addSubcommand(new HistoryCommand(samplePriceHistory()));
        int exitCode = cli.execute(args);
        System.exit(exitCode);
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
     * Статистики, которые печатает {@code stats} (семинар 6). Добавить новую — написать класс,
     * реализующий {@link Stat}, в {@code stat.impl}, и дописать его сюда; сам {@code StatsCommand}
     * не меняется — ровно тот же принцип, что у {@code InstrumentFilter}.
     *
     * <p>NOTICE для семинариста — порядок списка соответствует этапам занятия:
     * <ul>
     *     <li>первые шесть — базовые статистики на дефолтных операциях стрима;</li>
     *     <li>{@code ParallelAverageChangeStat} — многопоточные стримы, кастомный {@code Spliterator}
     *     ({@code spliterator/ArraySpliterator.java});</li>
     *     <li>последние две — Gatherer API: встроенный ({@code Gatherers.windowSliding}) и
     *     кастомный ({@code gather/MovingAverageGatherer.java}) на одной и той же формуле.</li>
     * </ul>
     * Всё перечисленное уже реализовано как эталон — вживую на занятии пишется заново тот же код,
     * эталон остаётся под рукой для сверки.
     */
    private static List<Stat<?>> stats() {
        return List.of(
                new CountByTypeStat(),
                new CountByCurrencyStat(),
                new AverageChangeStat(),
                new TotalPriceStat(),
                new GainLossSplitStat(),
                new TopGainerStat(),
                new ParallelAverageChangeStat(),
                new WindowedAveragePriceStat(),
                new MovingAveragePriceStat());
    }

    /**
     * Заглушка истории цен по тикерам — для команды {@code history} (скользящее среднее через
     * Gatherer API, семинар 6). Отдельная {@code Map}, не часть {@code QuoteSource}: истории нет у
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
