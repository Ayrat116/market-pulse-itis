package academy.backend.market_pulse.exception;

import academy.backend.market_pulse.dictionary.InstrumentType;

/**
 * Инструмент запрошенного типа не поддерживается — для него нет зарегистрированной фабрики.
 */
public class UnsupportedInstrumentException extends RuntimeException {

    public UnsupportedInstrumentException(InstrumentType type) {
        super("Инструмент такого типа не поддерживается: " + type);
    }
}
