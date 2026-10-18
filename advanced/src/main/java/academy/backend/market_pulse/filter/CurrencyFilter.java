package academy.backend.market_pulse.filter;

import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CurrencyFilter implements InstrumentFilter {

    private final Currency currency;

    @Override
    public boolean matches(Instrument instrument) {
        return instrument.getCurrency() == currency;
    }
}
