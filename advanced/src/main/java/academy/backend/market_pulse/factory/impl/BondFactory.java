package academy.backend.market_pulse.factory.impl;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.factory.InstrumentFactory;
import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Year;

public class BondFactory implements InstrumentFactory {

    @Getter
    private final InstrumentType type = InstrumentType.BOND;

    @Override
    public Instrument create(String ticker, String name, Currency currency) {
        // couponRate и maturityYear не собираются через CLI — используются значения по умолчанию.
        return new Bond(ticker, name, currency, BigDecimal.ZERO, Year.now().getValue());
    }
}
