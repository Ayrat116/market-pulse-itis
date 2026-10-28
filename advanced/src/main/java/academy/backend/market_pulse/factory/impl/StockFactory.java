package academy.backend.market_pulse.factory.impl;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.factory.InstrumentFactory;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Stock;
import lombok.Getter;

import java.math.BigDecimal;

public class StockFactory implements InstrumentFactory {

    @Getter
    private final InstrumentType type = InstrumentType.STOCK;

    @Override
    public Instrument create(String ticker, String name, Currency currency) {
        // sector и dividendYield не собираются через CLI — используются значения по умолчанию,
        // уточняются последующим редактированием инструмента.
        return new Stock(ticker, name, currency, "Unspecified", BigDecimal.ZERO);
    }
}
