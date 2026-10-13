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
 * То же скользящее среднее цены по окнам из {@value #WINDOW_SIZE}, что в
 * {@link WindowedAveragePriceStat}, но через кастомный {@link MovingAverageGatherer}: окно и сумма
 * — состояние {@code integrator}, среднее считается инкрементально, без материализации
 * списков-окон.
 */
public class MovingAveragePriceStat implements Stat<List<BigDecimal>> {

    private static final int WINDOW_SIZE = 3;

    @Getter
    private final StatCode code = StatCode.MOVING_AVERAGE_PRICE;

    @Override
    public List<BigDecimal> calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .map(Quote::getPrice)
                .gather(MovingAverageGatherer.movingAverage(WINDOW_SIZE))
                .toList();
    }
}
