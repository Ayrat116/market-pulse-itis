package academy.backend.market_pulse.factory;

import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.aware.InstrumentTypeAware;


public interface InstrumentFactory extends InstrumentTypeAware {

    Instrument create(String ticker, String name, Currency currency);
}
