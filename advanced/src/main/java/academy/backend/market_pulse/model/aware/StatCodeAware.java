package academy.backend.market_pulse.model.aware;

import academy.backend.market_pulse.dictionary.StatCode;

/**
 * Тот же приём, что {@link InstrumentTypeAware} у {@code InstrumentFactory}: самоидентификация
 * через код вместо {@code instanceof}/рефлексии, когда нужно подписать или различить реализацию.
 */
public interface StatCodeAware {

    StatCode getCode();
}
