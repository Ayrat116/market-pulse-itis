package academy.backend.market_pulse.model.aware;

import java.math.BigDecimal;

/**
 * Инструменты, для которых определены формулы расчета дивидендов.
 */
public interface DividendsAware {

    BigDecimal getDividends(BigDecimal currentPrice);
}
