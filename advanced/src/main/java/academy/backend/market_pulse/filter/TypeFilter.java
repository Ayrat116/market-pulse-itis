package academy.backend.market_pulse.filter;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.model.Instrument;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TypeFilter implements InstrumentFilter {

    private final InstrumentType type;

    @Override
    public boolean matches(Instrument instrument) {
        return instrument.getType() == type;
    }
}
