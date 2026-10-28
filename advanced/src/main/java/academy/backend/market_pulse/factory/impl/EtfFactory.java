package academy.backend.market_pulse.factory.impl;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.factory.InstrumentFactory;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Instrument;
import lombok.Getter;

public class EtfFactory implements InstrumentFactory {

    @Getter
    private final InstrumentType type = InstrumentType.ETF;

    @Override
    public Instrument create(String ticker, String name, Currency currency) {
        // trackingIndex не собирается через CLI — используется значение по умолчанию.
        return new Etf(ticker, name, currency, "Unspecified");
    }
}
