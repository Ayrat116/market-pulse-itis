package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.cli.AddCommand;
import academy.backend.market_pulse.cli.ListCommand;
import academy.backend.market_pulse.cli.MarketPulseCli;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Stock;
import academy.backend.market_pulse.repository.InMemoryInstrumentRepository;
import academy.backend.market_pulse.repository.InstrumentRepository;
import academy.backend.market_pulse.util.InstrumentValidatorFactory;
import lombok.SneakyThrows;
import picocli.CommandLine;

import java.math.BigDecimal;

/**
 * Запускает сценарии с ошибками. Первые два идут через CLI-команду {@code add}: она ловит предметные
 * исключения ({@code InvalidInstrumentException}, {@code DuplicateTickerException}) и печатает
 * понятное сообщение вместо стектрейса. Третий — невалидный инструмент вне CLI: его никто не
 * перехватывает (единой точки обработки в {@code Main} нет), и исключение завершает программу.
 */
public class CommandFailuresDemo {

    @SneakyThrows
    public static void main(String[] args) {
        InstrumentRepository repository = new InMemoryInstrumentRepository();
        CommandLine cli = new CommandLine(new MarketPulseCli())
                .addSubcommand(new AddCommand(repository))
                .addSubcommand(new ListCommand(repository));

        System.out.println("=== Неподдерживаемыая комманда ===");
        cli.execute("test");
        Thread.sleep(1000);
        System.out.println();

        System.out.println("=== Невалидный инструмент: пустой тикер ===");
        cli.execute("add", "STOCK", "", "Без тикера", "RUB");
        Thread.sleep(1000);
        System.out.println();

        System.out.println("=== Повторное добавление уже существующего тикера ===");
        // SBER уже есть в стартовых данных InMemoryInstrumentRepository — findByTicker(...) найдёт
        // его, и AddCommand превратит DuplicateTickerException в сообщение пользователю.
        cli.execute("add", "STOCK", "SBER", "Сбербанк", "RUB");
        Thread.sleep(1000);
        System.out.println();

        System.out.println("=== Невалидный инструмент: создание напрямую, в обход CLI ===");
        // Через add так не получится: StockFactory сама подставляет sector/dividendYield по
        // умолчанию, CLI их вообще не спрашивает. Здесь создаём Stock напрямую и прогоняем через
        // валидатор — InvalidInstrumentException здесь никто не ловит, и она завершает программу.
        InstrumentValidatorFactory.validate(
                new Stock("CRYPTO", "Крипта", Currency.RUB, "", BigDecimal.ZERO));
    }
}
