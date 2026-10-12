package academy.backend.market_pulse.dictionary;

/**
 * Код статистики — идентифицирует конкретную реализацию {@code Stat} в выводе команды
 * {@code stats} (семинар 6). Та же роль, что у {@link InstrumentType} для {@code InstrumentFactory}:
 * не завязываться на {@code getClass().getSimpleName()} там, где нужно подписать результат.
 */
public enum StatCode {
    COUNT_BY_TYPE,
    COUNT_BY_CURRENCY,
    AVERAGE_CHANGE,
    TOTAL_PRICE,
    GAIN_LOSS_SPLIT
}
