package academy.backend.market_pulse.model;

import academy.backend.market_pulse.dictionary.InstrumentType;
import lombok.Getter;

/**
 * ETF (exchange-traded fund) — биржевой фонд: торгуемая на бирже корзина активов,
 * которая отслеживает динамику индекса или сектора рынка.
 */
@Getter
public class Etf extends Instrument {

    private final InstrumentType type = InstrumentType.ETF;

    private final String trackingIndex;

    public Etf(String ticker, String name, Currency currency, String trackingIndex) {
        super(ticker, name, currency);
        this.trackingIndex = trackingIndex;
    }

    @Override
    public String getDescription() {
        return "ETF, отслеживает индекс: " + trackingIndex;
    }
}
