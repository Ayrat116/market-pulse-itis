package academy.backend.market_pulse.model;

import academy.backend.market_pulse.model.aware.InstrumentTypeAware;
import lombok.Getter;

/**
 * Базовая абстракция финансового инструмента. Инкапсулирует общие для всех
 * инструментов данные (тикер, название, валюта). Корректность данных конструктор не проверяет —
 * инструмент валидируется после создания, в {@code InstrumentFactories.create()}
 * (см. {@code InstrumentValidatorFactory}).
 */
@Getter
public abstract class Instrument implements InstrumentTypeAware {

    private final String ticker;
    private final String name;
    private final Currency currency;

    public Instrument(String ticker, String name, Currency currency) {
        this.ticker = ticker;
        this.name = name;
        this.currency = currency;
    }

    public abstract String getDescription();

    @Override
    public String toString() {
        return "%s(%s)".formatted(getClass().getSimpleName(), ticker);
    }
}
