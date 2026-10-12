package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.gather.MovingAverageGatherer;
import academy.backend.market_pulse.gather.SlidingSpreadGatherer;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Скользящее среднее и скользящий разброс (max - min) по истории цен инструмента через Gatherer
 * API (семинар 6: {@link MovingAverageGatherer}, {@link SlidingSpreadGatherer}). История цен —
 * заглушка-{@code Map} в {@code Main}, а не реальный источник.
 */
@Command(name = "history", description = "Скользящее среднее и скользящий разброс по истории цен инструмента")
@RequiredArgsConstructor
public class HistoryCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Тикер инструмента")
    private String ticker;

    @Option(names = "--sma", description = "Окно скользящего среднего (по умолчанию 3)")
    private int window = 3;

    @Option(names = "--spread", description = "Показать скользящий разброс (max - min) вместо среднего")
    private boolean spread;

    private final Map<String, List<BigDecimal>> priceHistory;

    @Override
    public Integer call() {
        List<BigDecimal> prices = priceHistory.get(ticker.toUpperCase());
        if (prices == null) {
            System.out.println("Нет истории цен по тикеру: " + ticker.toUpperCase());
            return 1;
        }

        System.out.println("Цены: " + prices);
        if (spread) {
            List<BigDecimal> spreadByWindow = prices.stream()
                    .gather(SlidingSpreadGatherer.spread(window))
                    .toList();
            System.out.println("Разброс(" + window + "): " + spreadByWindow);
        } else {
            List<BigDecimal> sma = prices.stream()
                    .gather(MovingAverageGatherer.movingAverage(window))
                    .toList();
            System.out.println("SMA(" + window + "): " + sma);
        }
        return 0;
    }
}
