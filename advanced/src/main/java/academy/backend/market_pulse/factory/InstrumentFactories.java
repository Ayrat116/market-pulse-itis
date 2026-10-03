package academy.backend.market_pulse.factory;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.exception.UnsupportedInstrumentException;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.util.InstrumentValidatorFactory;
import academy.backend.market_pulse.util.Registry;
import lombok.experimental.UtilityClass;

/**
 * Фабрика, реализованная с помощью SPI.
 */
@UtilityClass
public final class InstrumentFactories {

    private static final Registry<InstrumentType, InstrumentFactory> REGISTRY = load();

    public static Instrument create(InstrumentType type, String ticker, String name, Currency currency) {
        InstrumentFactory factory = REGISTRY.get(type).orElse(null);
        if (factory == null) {
            throw new UnsupportedInstrumentException(type);
        }
        var instrument = factory.create(ticker, name, currency);
        InstrumentValidatorFactory.validate(instrument);
        return instrument;
    }

    private static Registry<InstrumentType, InstrumentFactory> load() {
        return new Registry<>(InstrumentFactory::getType) {
        };
    }
}
