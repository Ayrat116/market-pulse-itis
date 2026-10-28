package academy.backend.market_pulse.exercise;

import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;

/**
 * Эталонное решение {@link StreamExercises} — для сверки семинаристом по ходу этапа 4; студентам
 * заранее не показывать (см. «План семинара.md», семинар 6, раздел «Что уже реализовано»). Не подключён ни в {@code Main},
 * ни в CLI — назначение ровно одно: {@code StreamExercisesReferenceTest} проверяет, что сам тест
 * (и формулировки упражнений) корректны, прежде чем раздавать {@link StreamExercisesSkeleton}
 * студентам.
 */
public class StreamExercisesReference implements StreamExercises {

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
        return quotes.stream()
                .anyMatch(quote -> quote.getChangePercent().signum() < 0);
    }

    @Override
    public long countByType(List<Quote> quotes, String instrumentType) {
        return quotes.stream()
                .filter(quote -> quote.getInstrument().getType().equals(instrumentType))
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
    public Map<String, Long> countByTypeGrouped(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        quote -> quote.getInstrument().getType().name(),
                        Collectors.counting()));
    }

    @Override
    public Map<String, Double> averageChangeByType(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        quote -> quote.getInstrument().getType().name(),
                        Collectors.averagingDouble(quote -> quote.getChangePercent().doubleValue())));
    }

    @Override
    public Map<Boolean, List<Quote>> partitionByGain(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.partitioningBy(quote -> quote.getChangePercent().signum() > 0));
    }

    @Override
    public Map<String, Quote> latestByTicker(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.toMap(
                        quote -> quote.getInstrument().getTicker(),
                        quote -> quote,
                        (existing, incoming) -> existing.getPrice().compareTo(incoming.getPrice()) >= 0
                                ? existing
                                : incoming));
    }

    @Override
    public String joinTickers(List<Quote> quotes) {
        return quotes.stream()
                .map(quote -> quote.getInstrument().getTicker())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public Map<Currency, String> tickersJoinedByCurrency(List<Quote> quotes) {
        return quotes.stream()
                .collect(Collectors.groupingBy(
                        quote -> quote.getInstrument().getCurrency(),
                        Collectors.mapping(
                                quote -> quote.getInstrument().getTicker(),
                                Collectors.joining(", "))));
    }

    @Override
    public List<Quote> findByTickers(List<Quote> quotes, List<String> tickers) {
        Map<String, Quote> byTicker = latestByTicker(quotes);
        return tickers.stream()
                .map(ticker -> Optional.ofNullable(byTicker.get(ticker)))
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
                        .divide(BigDecimal.valueOf(windowSize), 2, RoundingMode.HALF_UP))
                .toList();
    }

    @Override
    public int longestLossStreak(List<Quote> quotes) {
        Gatherer<Quote, StreakState, Integer> trackLossStreaks = Gatherer.ofSequential(
                StreakState::new,
                Gatherer.Integrator.<StreakState, Quote, Integer>ofGreedy((state, quote, downstream) -> {
                    if (quote.getChangePercent().signum() < 0) {
                        state.current++;
                    } else if (state.current > 0) {
                        downstream.push(state.current);
                        state.current = 0;
                    }
                    return true;
                }),
                (state, downstream) -> {
                    if (state.current > 0) {
                        downstream.push(state.current);
                    }
                });

        return quotes.stream()
                .gather(trackLossStreaks)
                .max(Comparator.naturalOrder())
                .orElse(0);
    }

    /** Состояние {@link #longestLossStreak}: длина серии убыточных котировок, идущей прямо сейчас. */
    private static final class StreakState {
        private int current = 0;
    }
}
