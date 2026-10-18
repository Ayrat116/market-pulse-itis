package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.config.AppConfig;
import picocli.CommandLine;

/**
 * Сборка {@link CommandLine} из готовых зависимостей {@link AppConfig}.
 */
public final class CommandLineFactory {

    private CommandLineFactory() {
    }

    public static CommandLine create(AppConfig config) {
        // NOTICE: подкоманды регистрируются вручную через addSubcommand — у них нет
        //  конструктора без аргументов, каждой нужны уже готовые зависимости.
        // NOTICE: единой точки обработки непредвиденных исключений сегодня нет — каждая команда
        //  (если вообще) ловит только свои ожидаемые типы. Аналог такой точки в Spring —
        //  @ControllerAdvice/@ExceptionHandler; здесь её пока нет, это открытый вопрос на будущее.
        return new CommandLine(new MarketPulseCli())
                .addSubcommand(new AddCommand(config.getInstrumentRepository()))
                .addSubcommand(new ListCommand(config.getInstrumentRepository()))
                .addSubcommand(new SaveCommand(config.getInstrumentRepository()))
                .addSubcommand(new WatchCommand(config.getWatchlist()))
                .addSubcommand(new QuoteCommand(config.getQuoteService()))
                .addSubcommand(new RemoveCommand(config.getInstrumentRepository(), config.getWatchlist()))
                .addSubcommand(new StatsCommand(config.getQuoteSource(), config.getStats()))
                .addSubcommand(new CompareCommand(config.getQuoteService()))
                .addSubcommand(new MoversCommand(config.getQuoteSource()))
                .addSubcommand(new HistoryCommand(config.getPriceHistory()))
                .addSubcommand(new UpdateCommand(config.getApiClient()));
    }
}
