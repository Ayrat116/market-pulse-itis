package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.model.Quote;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Забирает актуальные котировки у {@code instruments-service} через {@link TInvestApiClient} и
 * печатает их. Какая именно реализация клиента стоит за интерфейсом — решает композиционный корень
 * (см. {@code AppConfig}), команде это неважно.
 */
@Command(name = "update", description = "Получить актуальные котировки у instruments-service")
@RequiredArgsConstructor
public class UpdateCommand implements Callable<Integer> {

    private final TInvestApiClient client;

    @Override
    public Integer call() {
        try {
            List<Quote> quotes = client.fetchQuotes();
            quotes.forEach(System.out::println);
            System.out.println("Получено котировок: " + quotes.size());
            return 0;
        } catch (TInvestApiException e) {
            System.out.println("Не удалось обновить котировки: " + e.getMessage());
            return 1;
        }
    }
}
