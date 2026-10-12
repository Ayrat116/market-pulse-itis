package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Суммарная цена всех котировок — через {@code reduce} над {@code BigDecimal}, а не
 * {@code mapToDouble().sum()}: это деньги, они не должны терять точность округлением в
 * {@code double} (см. {@link AverageChangeStat}, где такая потеря уже допустима).
 */
public class TotalPriceStat implements Stat<BigDecimal> {

    @Getter
    private final StatCode code = StatCode.TOTAL_PRICE;

    @Override
    public BigDecimal calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .map(Quote::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
