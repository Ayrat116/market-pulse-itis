package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.gather.MovingAverageGatherer;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// NOTICE: семинарист пишет этот класс вживую — кастомный Gatherer (тот же MovingAverageGatherer,
// что уже использует history TICKER --sma K), в паре с предыдущим (встроенный windowSliding).
/**
 * Переписанный {@link NaiveMovingAveragePriceStat}: та же формула — скользящее среднее цены по
 * окнам из {@value #WINDOW_SIZE} — но через кастомный {@link MovingAverageGatherer}. Окно и сумма
 * — состояние {@code integrator}, среднее считается инкрементально за один проход, без
 * {@code subList} и без пересчёта заново на каждое окно — см. {@link NaiveMovingAveragePriceStat}
 * для версии «в лоб» на базовых операциях и {@link WindowedAveragePriceStat} — промежуточную
 * версию на встроенном {@code Gatherers.windowSliding}.
 */
public class MovingAveragePriceStat implements Stat<List<BigDecimal>> {

    private static final int WINDOW_SIZE = 3;

    @Getter
    private final StatCode code = StatCode.MOVING_AVERAGE_PRICE;

    @Override
    public List<BigDecimal> calculateStats(List<Quote> quotes) {
        // NOTICE: MovingAverageGatherer.movingAverage(WINDOW_SIZE) — тот же класс, что уже
        // использует history --sma. Переписывает NaiveMovingAveragePriceStat (этап 3) — подчеркнуть
        // разницу вслух: один проход по стриму, без subList, без пересчёта среднего заново на
        // каждое окно.
        // TODO: семинар 6, этап 5 — gather(MovingAverageGatherer.movingAverage(WINDOW_SIZE))
        throw new UnsupportedOperationException("MovingAveragePriceStat ещё не реализован");
    }
}
