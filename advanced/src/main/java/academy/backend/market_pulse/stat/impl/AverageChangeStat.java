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
        return quotes.stream()
                .mapToDouble(quote -> quote.getChangePercent().doubleValue())
                .average()
                .orElse(0.0);
    }
}
