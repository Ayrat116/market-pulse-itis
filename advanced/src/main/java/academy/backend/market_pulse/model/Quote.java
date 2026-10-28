package academy.backend.market_pulse.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Рыночная котировка: инструмент + цена + изменение за период.
 * Использует агрегацию, а не наследование от {@link Stock}.
 */
@Getter
@RequiredArgsConstructor
public class Quote {

    private final Instrument instrument;
    private final BigDecimal price;
    private final BigDecimal changePercent;

    public BigDecimal getDividends() {
        if (instrument instanceof Stock stock) {
            return stock.getDividends(this.price);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        String direction = switch (changePercent.signum()) {
            case 1 -> "▲";
            case -1 -> "▼";
            default -> "▬";
        };
        return instrument.getTicker() + ": " + price + " " + instrument.getCurrency()
                + " " + direction + " " + changePercent.abs() + "%";
    }
}
