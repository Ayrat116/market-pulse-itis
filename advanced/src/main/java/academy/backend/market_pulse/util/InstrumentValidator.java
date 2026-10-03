package academy.backend.market_pulse.util;

import academy.backend.market_pulse.exception.InvalidInstrumentException;
import academy.backend.market_pulse.model.Bond;
import academy.backend.market_pulse.model.Instrument;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO: реализовать и интегрировать в InstrumentFactories.create()
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstrumentValidator<I extends Instrument> {

    public static final InstrumentValidator<Bond> BOND_VALIDATOR =
            InstrumentValidator.<Bond>builder()
                    .addValidationRule(
                            it -> !(it.getCouponRate() == null || it.getCouponRate().compareTo(BigDecimal.ZERO) < 0),
                            "Купонная ставка не может быть отрицательной");

    private final List<InstrumentValidatorDelegate> delegates = new ArrayList<>();

    public static <I extends Instrument> InstrumentValidator<I> builder() {
        return new InstrumentValidator<>();
    }

    public InstrumentValidator<I> addValidationRule(Predicate<I> predicate, Supplier<String> descriptionSupplier) {
        delegates.add(new InstrumentValidatorDelegate(predicate, descriptionSupplier));
        return this;
    }

    public InstrumentValidator<I> addValidationRule(Predicate<I> predicate, String description) {
        delegates.add(new InstrumentValidatorDelegate(predicate, () -> description));
        return this;
    }

    public void validate(I instrument) {
        var violations = delegates.stream()
                .filter(it -> !it.isValid.test(instrument))
                .map(it -> it.descriptionSupplier.get())
                .collect(Collectors.toSet());
        if (!violations.isEmpty()) {
            throw new InvalidInstrumentException(
                    instrument.getType(),
                    String.join(", ", violations));
        }
    }

    @RequiredArgsConstructor
    private class InstrumentValidatorDelegate {
        private final Predicate<I> isValid;
        private final Supplier<String> descriptionSupplier;
    }
}
