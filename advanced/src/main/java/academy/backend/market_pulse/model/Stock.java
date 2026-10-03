package academy.backend.market_pulse.model;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.model.aware.DividendsAware;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Акция — долевая ценная бумага: удостоверяет право владельца на долю в капитале компании,
 * участие в управлении и получение части прибыли эмитента в виде дивидендов.
 */
@Getter
public class Stock extends Instrument implements DividendsAware {

    private final InstrumentType type = InstrumentType.STOCK;

    private final String sector;

    /**
     * Дивидендная доходность в процентах — единственный числовой атрибут акции,
     * используется, в том числе, как «аналог цены» в фильтрации ({@code PriceFilter}).
     */
    private final BigDecimal dividendYield;

    public Stock(String ticker, String name, Currency currency,
                 String sector, BigDecimal dividendYield) {
        super(ticker, name, currency);
        this.sector = sector;
        this.dividendYield = dividendYield;
    }

    @Override
    public String getDescription() {
        return "Акция, сектор: " + sector;
    }

    /**
     * Годовая дивидендная доходность в валюте инструмента: цена × доходность / 100.
     */
    @Override
    public BigDecimal getDividends(BigDecimal currentPrice) {
        return currentPrice.multiply(dividendYield)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
