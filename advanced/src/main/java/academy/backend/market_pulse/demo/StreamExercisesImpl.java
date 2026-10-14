package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamExercisesImpl implements StreamExercises {

    @Override
    public List<String> tickersInCurrency(List<Quote> quotes, Currency currency) {
        // TODO: семинар 6, упражнение 1
        throw new UnsupportedOperationException("Упражнение 1 ещё не реализовано");
    }

    @Override
    public List<Quote> topByPrice(List<Quote> quotes, int n) {
        // TODO: семинар 6, упражнение 2
        throw new UnsupportedOperationException("Упражнение 2 ещё не реализовано");
    }

    @Override
    public Set<Currency> distinctCurrencies(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 3
        throw new UnsupportedOperationException("Упражнение 3 ещё не реализовано");
    }

    @Override
    public boolean hasAnyLoser(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 4
        throw new UnsupportedOperationException("Упражнение 4 ещё не реализовано");
    }

    @Override
    public long countByType(List<Quote> quotes, InstrumentType instrumentType) {
        // TODO: семинар 6, упражнение 5
        throw new UnsupportedOperationException("Упражнение 5 ещё не реализовано");
    }

    @Override
    public BigDecimal totalPrice(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 6
        throw new UnsupportedOperationException("Упражнение 6 ещё не реализовано");
    }

    @Override
    public double averageChangePercent(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 7
        throw new UnsupportedOperationException("Упражнение 7 ещё не реализовано");
    }

    @Override
    public Map<InstrumentType, Long> countByTypeGrouped(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 8
        throw new UnsupportedOperationException("Упражнение 8 ещё не реализовано");
    }

    @Override
    public Map<InstrumentType, Double> averageChangeByType(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 9
        throw new UnsupportedOperationException("Упражнение 9 ещё не реализовано");
    }

    @Override
    public Map<Boolean, List<Quote>> partitionByGain(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 10
        throw new UnsupportedOperationException("Упражнение 10 ещё не реализовано");
    }

    @Override
    public Map<String, Quote> latestByTicker(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 11
        throw new UnsupportedOperationException("Упражнение 11 ещё не реализовано");
    }

    @Override
    public String joinTickers(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 12
        throw new UnsupportedOperationException("Упражнение 12 ещё не реализовано");
    }

    @Override
    public Map<Currency, String> tickersJoinedByCurrency(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 13
        throw new UnsupportedOperationException("Упражнение 13 ещё не реализовано");
    }

    @Override
    public List<Quote> findByTickers(List<Quote> quotes, List<String> tickers) {
        // TODO: семинар 6, упражнение 14
        throw new UnsupportedOperationException("Упражнение 14 ещё не реализовано");
    }

    @Override
    public List<BigDecimal> movingAveragePrice(List<Quote> quotes, int windowSize) {
        // TODO: семинар 6, упражнение 15 (Gatherer API)
        throw new UnsupportedOperationException("Упражнение 15 ещё не реализовано");
    }

    @Override
    public int longestLossStreak(List<Quote> quotes) {
        // TODO: семинар 6, упражнение 16 (Gatherer API)
        throw new UnsupportedOperationException("Упражнение 16 ещё не реализовано");
    }
}
