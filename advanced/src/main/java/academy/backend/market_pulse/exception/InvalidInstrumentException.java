package academy.backend.market_pulse.exception;

import academy.backend.market_pulse.dictionary.InstrumentType;

/**
 * Инструмент создан, но не проходит бизнес-валидацию {@code InstrumentValidator} — бросается
 * после создания сущности в фабрике, а не на входных параметрах команды.
 * TODO №1: требует реализации и применения!
 * TODO №2: добавить билдер для сбора ошибок валидации
 */
public class InvalidInstrumentException extends RuntimeException {

    public InvalidInstrumentException(InstrumentType type, String description) {
        super(
                "Ошибка валидации инструмента типа %s: %s"
                        .formatted(type.name(), description));
    }
}
