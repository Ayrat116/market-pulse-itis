package academy.backend.market_pulse.filter;

import academy.backend.market_pulse.model.Instrument;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TickerFilter implements InstrumentFilter {

    private final String query;

    @Override
    public boolean matches(Instrument instrument) {
        return instrument.getTicker().toUpperCase().contains(query.toUpperCase());
    }
}
