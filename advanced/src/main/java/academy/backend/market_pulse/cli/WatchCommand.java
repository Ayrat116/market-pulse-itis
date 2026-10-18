package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.model.Watchlist;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "watch", description = "Отслеживать инструмент: добавить тикер в watchlist или показать текущий список")
@RequiredArgsConstructor
public class WatchCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "Тикер инструмента (без аргумента — показать watchlist)")
    private String ticker;

    private final Watchlist watchlist;

    @Override
    public Integer call() {
        if (ticker != null) {
            boolean added = watchlist.add(ticker);
            System.out.println(added
                    ? "Добавлено в watchlist: " + ticker.toUpperCase()
                    : "Уже отслеживается: " + ticker.toUpperCase());
        }
        System.out.println("Watchlist: " + watchlist.tickers());
        return 0;
    }
}
