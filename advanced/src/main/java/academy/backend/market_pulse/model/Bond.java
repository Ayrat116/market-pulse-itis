package academy.backend.market_pulse.model;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.exception.InvalidInstrumentException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Year;

/**
 * Облигация — долговая ценная бумага: эмитент занимает деньги у владельца облигации
 * и обязуется выплачивать купонный доход, а в дату погашения — вернуть номинал.
 */
@Getter
public class Bond extends Instrument {

    private final InstrumentType type = InstrumentType.BOND;

    private final BigDecimal couponRate;
    private final int maturityYear;

    public Bond(String ticker, String name, Currency currency,
                BigDecimal couponRate, int maturityYear) {
        super(ticker, name, currency);
        if (couponRate == null || couponRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidInstrumentException(
                    InstrumentType.BOND,
                    "Купонная ставка не может быть отрицательной");
        }
        if (maturityYear < Year.now().getValue()) {
            throw new IllegalArgumentException("Год погашения не может быть в прошлом");
        }
        this.couponRate = couponRate;
        this.maturityYear = maturityYear;
    }

    @Override
    public String getDescription() {
        return "Облигация, купон: " + couponRate + "%, погашение: " + maturityYear;
    }
}
