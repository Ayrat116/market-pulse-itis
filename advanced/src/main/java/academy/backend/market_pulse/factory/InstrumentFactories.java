package academy.backend.market_pulse.factory;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.util.Registry;
import lombok.experimental.UtilityClass;

import static academy.backend.market_pulse.util.InstrumentValidator.BOND_VALIDATOR;

/**
 * Фабрика, реализованная с помощью SPI.
 */
@UtilityClass
public final class InstrumentFactories {

    private static final Registry<InstrumentType, InstrumentFactory> REGISTRY = load();

    public static Instrument create(InstrumentType type, String ticker, String name, Currency currency) {
        InstrumentFactory factory = REGISTRY.get(type).orElse(null);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown instrument type: " + type);
        }
        // TODO: прогнать instrument через InstrumentValidator и бросить InvalidInstrumentException
        //  при нарушении бизнес-правил — валидируем уже созданную сущность, а не входные параметры.
        var instrument = factory.create(ticker, name, currency);
        BOND_VALIDATOR.validate((Bond) instrument);
        return instrument;
    }

    private static Registry<InstrumentType, InstrumentFactory> load() {
        return new Registry<>(InstrumentFactory::getType) {
        };
    }
}
