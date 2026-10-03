package academy.backend.market_pulse;

import academy.backend.market_pulse.cli.AddCommand;
import academy.backend.market_pulse.cli.ListCommand;
import academy.backend.market_pulse.cli.MarketPulseCli;
import academy.backend.market_pulse.cli.SaveCommand;
import academy.backend.market_pulse.repository.InMemoryInstrumentRepository;
import academy.backend.market_pulse.repository.InstrumentRepository;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        InstrumentRepository repository = new InMemoryInstrumentRepository();
        // NOTICE: подкоманды регистрируются вручную через addSubcommand — у них нет
        //  конструктора без аргументов, каждой нужен уже готовый repository.
        // NOTICE: единой точки обработки непредвиденных исключений сегодня нет — каждая команда
        //  (если вообще) ловит только свои ожидаемые типы. Аналог такой точки в Spring —
        //  @ControllerAdvice/@ExceptionHandler; здесь её пока нет, это открытый вопрос на будущее.
        CommandLine cli = new CommandLine(new MarketPulseCli())
                .addSubcommand(new AddCommand(repository))
                .addSubcommand(new ListCommand(repository))
                .addSubcommand(new SaveCommand(repository));
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }
}
