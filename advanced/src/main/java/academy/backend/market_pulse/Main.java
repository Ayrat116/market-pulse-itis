package academy.backend.market_pulse;

import academy.backend.market_pulse.cli.AddCommand;
import academy.backend.market_pulse.cli.ListCommand;
import academy.backend.market_pulse.cli.MarketPulseCli;
import academy.backend.market_pulse.cli.QuoteCommand;
import academy.backend.market_pulse.cli.RemoveCommand;
import academy.backend.market_pulse.cli.SaveCommand;
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
import picocli.CommandLine;

import java.math.BigDecimal;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        InstrumentRepository repository = new InMemoryInstrumentRepository();
        Watchlist watchlist = new Watchlist();
        QuoteService quoteService = new QuoteService(new QuoteSource(sampleQuotes()));

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
                .addSubcommand(new RemoveCommand(repository, watchlist));
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }

    /**
     * Заглушка внешнего источника котировок — заранее заготовленные данные вместо реального
     * сетевого запроса.
     */
    private static Map<String, Quote> sampleQuotes() {
        Instrument sber = InstrumentFactories.create(InstrumentType.STOCK, "SBER", "Сбербанк", Currency.RUB);
        Instrument gazp = InstrumentFactories.create(InstrumentType.STOCK, "GAZP", "Газпром", Currency.RUB);
        return Map.of(
                "SBER", new Quote(sber, new BigDecimal("250.00"), new BigDecimal("1.20")),
                "GAZP", new Quote(gazp, new BigDecimal("120.50"), new BigDecimal("-0.80")));
    }
}
