package academy.backend.market_pulse.exception;

/**
 * Ошибка сохранения инструментов на диск — оборачивает исходную причину (обычно
 * {@link java.io.IOException}), не теряя её.
 */
public class InstrumentSaveException extends RuntimeException {

    public InstrumentSaveException(String path, Throwable cause) {
        super("Не удалось сохранить инструменты в файл %s: %s".formatted(path, cause.getMessage()), cause);
    }
}
