package academy.backend.market_pulse.util;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.exception.UnsupportedInstrumentException;
import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Etf;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Stock;
import lombok.experimental.UtilityClass;

import java.time.Year;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Подбирает {@link InstrumentValidator} под тип инструмента. Общие для всех типов правила (тикер,
 * название, валюта) добавляются в каждый валидатор, правила конкретного типа — поверх них.
 */
@UtilityClass
public final class InstrumentValidatorFactory {

    private static final Map<InstrumentType, Consumer<Instrument>> VALIDATORS = Map.of(
            InstrumentType.STOCK, adapt(Stock.class, stockValidator()),
            InstrumentType.BOND, adapt(Bond.class, bondValidator()),
            InstrumentType.ETF, adapt(Etf.class, etfValidator()));

    /**
     * @throws academy.backend.market_pulse.exception.InvalidInstrumentException если инструмент
     *                                                                           нарушает правила своего типа
     * @throws UnsupportedInstrumentException                                    если для типа нет валидатора
     */
    public static void validate(Instrument instrument) {
        var validator = VALIDATORS.get(instrument.getType());
        if (validator == null) {
            throw new UnsupportedInstrumentException(instrument.getType());
        }
        validator.accept(instrument);
    }

    private static <I extends Instrument> Consumer<Instrument> adapt(Class<I> type, InstrumentValidator<I> validator) {
        return instrument -> validator.validate(type.cast(instrument));
    }

    private static <I extends Instrument> InstrumentValidator<I> common() {
        return InstrumentValidator.<I>builder()
                .addValidationRule(
                        it -> it.getTicker() != null && !it.getTicker().isBlank(),
                        "Тикер не может быть пустым")
                .addValidationRule(
                        it -> it.getName() != null && !it.getName().isBlank(),
                        "Название инструмента не может быть пустым")
                .addValidationRule(
                        it -> it.getCurrency() != null,
                        "Валюта инструмента обязательна");
    }

    private static InstrumentValidator<Stock> stockValidator() {
        return InstrumentValidatorFactory.<Stock>common()
                .addValidationRule(
                        it -> it.getSector() != null && !it.getSector().isBlank(),
                        "Сектор не может быть пустым")
                .addValidationRule(
                        it -> it.getDividendYield() != null && it.getDividendYield().signum() >= 0,
                        "Дивидендная доходность не может быть отрицательной");
    }

    private static InstrumentValidator<Bond> bondValidator() {
        return InstrumentValidatorFactory.<Bond>common()
                .addValidationRule(
                        it -> it.getCouponRate() != null && it.getCouponRate().signum() >= 0,
                        "Купонная ставка не может быть отрицательной")
                .addValidationRule(
                        it -> it.getMaturityYear() >= Year.now().getValue(),
                        "Год погашения не может быть в прошлом");
    }

    private static InstrumentValidator<Etf> etfValidator() {
        return InstrumentValidatorFactory.<Etf>common()
                .addValidationRule(
                        it -> it.getTrackingIndex() != null && !it.getTrackingIndex().isBlank(),
                        "Отслеживаемый индекс не может быть пустым");
    }
}
