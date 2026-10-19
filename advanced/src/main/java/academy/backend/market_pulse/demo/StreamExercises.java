package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 16 упражнений на Stream API над списком котировок — от базовых операций до Gatherer API, от
 * лёгких к сложным. У каждого метода — своя формулировка задачи в javadoc; решения —
 * в {@link StreamExercisesImpl}.
 */
public interface StreamExercises {

    // ---------------------------------------------------------------------
    // Лёгкие: один проход, одна-две промежуточные операции
    // ---------------------------------------------------------------------

    /**
     * Упражнение 1. Тикеры инструментов, котирующихся в заданной валюте — в исходном порядке
     * списка. ({@code filter} + {@code map} + {@code toList}).
     */
    List<String> tickersInCurrency(List<Quote> quotes, Currency currency);

    /**
     * Упражнение 2. {@code n} самых дорогих котировок по убыванию цены. ({@code sorted} +
     * {@code limit}).
     */
    List<Quote> topByPrice(List<Quote> quotes, int n);

    /**
     * Упражнение 3. Валюты, представленные в списке котировок, без повторов. ({@code map} +
     * {@code distinct}).
     */
    Set<Currency> distinctCurrencies(List<Quote> quotes);

    /**
     * Упражнение 4. Есть ли в списке хотя бы одна просевшая котировка ({@code changePercent < 0}).
     * Короткозамкнутая операция: не должна обходить весь список, если просевшая нашлась в начале.
     */
    boolean hasAnyLoser(List<Quote> quotes);

    /**
     * Упражнение 5. Сколько котировок в списке относится к инструментам заданного типа.
     * ({@code filter} + {@code count}).
     */
    long countByType(List<Quote> quotes, InstrumentType instrumentType);

    // ---------------------------------------------------------------------
    // Средние: reduce, числовые стримы, многоключевые коллекторы
    // ---------------------------------------------------------------------

    /**
     * Упражнение 6. Суммарная цена всех котировок. Через {@code reduce} над {@code BigDecimal}, а не
     * {@code mapToDouble().sum()} — деньги не должны терять точность округлением в {@code double}.
     */
    BigDecimal totalPrice(List<Quote> quotes);

    /**
     * Упражнение 7. Средняя доходность ({@code changePercent}) по всем котировкам, {@code 0.0} для
     * пустого списка. ({@code mapToDouble} + {@code average}).
     */
    double averageChangePercent(List<Quote> quotes);

    /**
     * Упражнение 8. Количество котировок по типу инструмента. ({@code groupingBy} + {@code counting}).
     */
    Map<InstrumentType, Long> countByTypeGrouped(List<Quote> quotes);

    /**
     * Упражнение 9. Средняя доходность отдельно по каждому типу инструмента — то же разбиение, что
     * в упражнении 8, но с другим результатом внутри группы. ({@code groupingBy} с
     * {@code averagingDouble} вторым коллектором).
     */
    Map<InstrumentType, Double> averageChangeByType(List<Quote> quotes);

    /**
     * Упражнение 10. Разбить котировки на выросшие ({@code changePercent > 0}) и все остальные —
     * ровно два ключа, {@code true}/{@code false}. ({@code partitioningBy}).
     */
    Map<Boolean, List<Quote>> partitionByGain(List<Quote> quotes);

    /**
     * Упражнение 11. Котировка по тикеру; если один и тот же тикер встречается в списке несколько
     * раз (например, цена обновилась) — оставить ту, у которой цена выше. ({@code toMap} с
     * merge-функцией для разрешения дубликатов ключа).
     */
    Map<String, Quote> latestByTicker(List<Quote> quotes);

    /**
     * Упражнение 12. Все тикеры из списка котировок одной строкой — через {@code ", "},
     * в квадратных скобках; дубликаты тикеров (как в упражнении 11) здесь не схлопывать, просто
     * перечислить все по порядку. ({@code map} + {@code Collectors.joining(delimiter, prefix,
     * suffix)}).
     */
    String joinTickers(List<Quote> quotes);

    /**
     * Упражнение 13. Для каждой валюты — строка со всеми её тикерами через {@code ", "} (без
     * квадратных скобок), без дублей-пропусков (дубликат тикера, если он есть, встречается в строке
     * дважды, как и в упражнении 12). Комбинация трёх коллекторов: {@code groupingBy(currency)}, у
     * которого downstream — {@code mapping(ticker, joining(", "))}: {@code mapping} сначала
     * проецирует элемент группы в тикер, и только потом {@code joining} их склеивает. Тот же приём,
     * что в {@code TickersByTypeStat} команды {@code stats}.
     */
    Map<Currency, String> tickersJoinedByCurrency(List<Quote> quotes);

    /**
     * Упражнение 14. Котировки по списку тикеров, в порядке списка тикеров; тикеры, которых нет в
     * {@code quotes}, молча пропускаются. ({@code map} в {@code Optional<Quote>} + {@code flatMap}
     * {@code (Optional::stream)} — тот же приём, что в {@code CompareCommand}).
     */
    List<Quote> findByTickers(List<Quote> quotes, List<String> tickers);

    // ---------------------------------------------------------------------
    // Сложные: Gatherer API — состояние между элементами конвейера
    // ---------------------------------------------------------------------

    /**
     * Упражнение 15. Скользящее среднее цены по окну {@code windowSize} подряд идущих котировок —
     * среднее округлено до 2 знаков ({@link java.math.RoundingMode#HALF_UP}). Решается через
     * {@code Gatherers.windowSliding(windowSize)} + {@code map}: встроенный gatherer собирает окна,
     * дальше остаётся только посчитать среднее в каждом. Тот же приём, что в
     * {@code WindowedAveragePriceStat} команды {@code stats}.
     */
    List<BigDecimal> movingAveragePrice(List<Quote> quotes, int windowSize);

    /**
     * Упражнение 16 (самое сложное). Длина самой длинной серии подряд идущих убыточных котировок
     * ({@code changePercent < 0}) во входном списке; {@code 0}, если убыточных котировок нет.
     * Классическая задача на состояние между соседними элементами — не выражается через
     * {@code map}/{@code filter}/{@code limit}: нужен счётчик текущей серии, который растёт на
     * убыточной котировке и сбрасывается на прибыльной/нулевой, плюс финальный учёт серии,
     * дошедшей до конца списка. Решается кастомным {@code Gatherer.ofSequential(initializer,
     * integrator)} с {@code finisher}.
     */
    int longestLossStreak(List<Quote> quotes);
}
