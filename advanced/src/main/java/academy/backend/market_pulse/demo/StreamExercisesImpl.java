package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;

public class StreamExercisesImpl implements StreamExercises {

    @Override
    public List<String> tickersInCurrency(List<Quote> quotes, Currency currency) {
        return quotes.stream()
                .filter(quote -> quote.getInstrument().getCurrency() == currency)
                .map(quote -> quote.getInstrument().getTicker())
                .toList();
    }

    @Override
    public List<Quote> topByPrice(List<Quote> quotes, int n) {
        return quotes.stream()
                .sorted(Comparator.comparing(Quote::getPrice).reversed())
                .limit(n)
                .toList();
    }

    @Override
    public Set<Currency> distinctCurrencies(List<Quote> quotes) {
        return quotes.stream()
                .map(quote -> quote.getInstrument().getCurrency())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasAnyLoser(List<Quote> quotes) {
        return quotes.stream().anyMatch(quote -> quote.getChangePercent().signum() < 0);
    }

    @Override
    public long countByType(List<Quote> quotes, InstrumentType instrumentType) {
        return quotes.stream()
                .filter(quote -> quote.getInstrument().getType() == instrumentType)
                .count();
    }

    @Override
    public BigDecimal totalPrice(List<Quote> quotes) {
        return quotes.stream()
                .map(Quote::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public double averageChangePercent(List<Quote> quotes) {
        return quotes.stream()
                .mapToDouble(quote -> quote.getChangePercent().doubleValue())
                .average()
                .orElse(0.0);
    }

    @Override
    public Map<InstrumentType, Long> countByTypeGrouped(List<Quote> quotes) {
        return quotes.stream().collect(Collectors.groupingBy(
                quote -> quote.getInstrument().getType(),
                Collectors.counting()));
    }

    @Override
    public Map<InstrumentType, Double> averageChangeByType(List<Quote> quotes) {
        return quotes.stream().collect(Collectors.groupingBy(
                quote -> quote.getInstrument().getType(),
                Collectors.averagingDouble(quote -> quote.getChangePercent().doubleValue())));
    }

    @Override
    public Map<Boolean, List<Quote>> partitionByGain(List<Quote> quotes) {
        return quotes.stream().collect(Collectors.partitioningBy(quote -> quote.getChangePercent().signum() > 0));
    }

    @Override
    public Map<String, Quote> latestByTicker(List<Quote> quotes) {
        return quotes.stream().collect(Collectors.toMap(
                quote -> quote.getInstrument().getTicker(),
                Function.identity(),
                (first, second) -> first.getPrice().compareTo(second.getPrice()) >= 0 ? first : second));
    }

    @Override
    public String joinTickers(List<Quote> quotes) {
        return quotes.stream()
                .map(quote -> quote.getInstrument().getTicker())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public Map<Currency, String> tickersJoinedByCurrency(List<Quote> quotes) {
        return quotes.stream().collect(Collectors.groupingBy(
                quote -> quote.getInstrument().getCurrency(),
                Collectors.mapping(quote -> quote.getInstrument().getTicker(), Collectors.joining(", "))));
    }

    @Override
    public List<Quote> findByTickers(List<Quote> quotes, List<String> tickers) {
        return tickers.stream()
                .map(ticker -> quotes.stream()
                        .filter(quote -> quote.getInstrument().getTicker().equals(ticker))
                        .findFirst())
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<BigDecimal> movingAveragePrice(List<Quote> quotes, int windowSize) {
        return quotes.stream()
                .map(Quote::getPrice)
                .gather(Gatherers.windowSliding(windowSize))
                .map(window -> window.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(window.size()), 2, RoundingMode.HALF_UP))
                .toList();
    }

    @Override
    public int longestLossStreak(List<Quote> quotes) {
        // состояние: [текущая серия, лучшая серия]
        Gatherer<Quote, int[], Integer> longestStreak = Gatherer.ofSequential(
                () -> new int[2],
                (state, quote, downstream) -> {
                    if (quote.getChangePercent().signum() < 0) {
                        state[0]++;
                        state[1] = Math.max(state[1], state[0]);
                    } else {
                        state[0] = 0;
                    }
                    return true;
                },
                (state, downstream) -> downstream.push(state[1]));
        return quotes.stream().gather(longestStreak).findFirst().orElse(0);
    }
}
