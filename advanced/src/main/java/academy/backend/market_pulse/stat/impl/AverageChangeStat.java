package academy.backend.market_pulse.stat.impl;

import academy.backend.market_pulse.dictionary.StatCode;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.stat.Stat;
import lombok.Getter;

import java.util.List;

/**
 * Средняя доходность по всем котировкам ({@code mapToDouble} + {@code average}). Перевод
 * {@code BigDecimal} в {@code double} теряет точность, но для отображаемого процента это
 * приемлемо — в отличие от {@link TotalPriceStat}, где считаются деньги.
 */
public class AverageChangeStat implements Stat<Double> {

    @Getter
    private final StatCode code = StatCode.AVERAGE_CHANGE;

    @Override
    public Double calculateStats(List<Quote> quotes) {
        // NOTICE: mapToDouble + average — перевод BigDecimal в double теряет точность, но для
        // отображаемого процента это приемлемо (контраст с TotalPriceStat, где считаются деньги).
        // TODO: семинар 6, этап 3 — mapToDouble + average
        throw new UnsupportedOperationException("AverageChangeStat ещё не реализован");
    }
}
