package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.gather.MovingAverageGatherer;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Gatherers;

/**
 * Встроенный {@code Gatherer} рядом с кастомным (семинар 6). {@code HistoryCommand} использует
 * только {@link MovingAverageGatherer} — этот демо-класс показывает, чем он отличается от
 * штатной альтернативы, на одних и тех же данных.
 */
public class GathererDemo {

    public static void main(String[] args) {
        List<BigDecimal> prices = List.of(
                new BigDecimal("244.00"), new BigDecimal("246.50"), new BigDecimal("245.00"),
                new BigDecimal("248.00"), new BigDecimal("250.00"), new BigDecimal("249.50"),
                new BigDecimal("251.20"));

        System.out.println("=============== Gatherers.windowSliding(3) — встроенный ===============");
        windowSliding(prices);

        System.out.println("=============== MovingAverageGatherer.movingAverage(3) — кастомный ===============");
        customMovingAverage(prices);
    }

    /**
     * Встроенный {@code windowSliding} сначала материализует каждое окно как отдельный
     * {@code List<BigDecimal>} — дальше нужен ещё один {@code map}, чтобы посчитать среднее.
     * Все {@code k} окон одновременно существуют в памяти как объекты списков.
     */
    private static void windowSliding(List<BigDecimal> prices) {
        prices.stream()
                .gather(Gatherers.windowSliding(3))
                .map(window -> window.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(window.size()), 2, java.math.RoundingMode.HALF_UP))
                .forEach(sma -> System.out.println("  окно -> среднее = " + sma));
    }

    /**
     * {@link MovingAverageGatherer} считает ровно тот же результат за один проход конвейера, без
     * промежуточного {@code map}: окно и сумма — внутреннее состояние {@code integrator}, среднее
     * считается и испускается сразу, как только окно заполнено (см. материал «Gatherer API»).
     */
    private static void customMovingAverage(List<BigDecimal> prices) {
        prices.stream()
                .gather(MovingAverageGatherer.movingAverage(3))
                .forEach(sma -> System.out.println("  SMA = " + sma));
    }
}
