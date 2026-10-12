package academy.backend.market_pulse.benchmark;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Quote;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Три способа посчитать среднюю доходность по одному и тому же списку котировок — обычный
 * {@code for}-each, последовательный стрим, параллельный стрим (семинар 6). Одна и та же формула
 * ({@code sum / count}, округлённая до {@code double}, как в {@code AverageChangeStat}), три разных
 * способа обойти коллекцию — разница в результатах бенчмарка приписывается только способу обхода,
 * а не вычислению. Результаты и выводы — в материале
 * {@code docs/materials/seminar6/Результаты ForEachVsStreamBenchmark.md}.
 *
 * <p>Параметр {@code size} показывает, как разница между тремя способами меняется с ростом объёма:
 * на малых коллекциях конвейер стрима (и тем более параллельного — создание задач в
 * {@code ForkJoinPool}) добавляет накладные расходы, которых нет у простого цикла; с ростом объёма
 * они перестают быть заметны на фоне самой работы.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class ForEachVsStreamBenchmark {

    @Param({"1000", "100000", "1000000"})
    private int size;

    private List<Quote> quotes;

    @Setup
    public void setup() {
        Instrument stock = InstrumentFactories.create(InstrumentType.STOCK, "SBER", "Сбербанк", Currency.RUB);
        Random random = new Random(42);
        quotes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            BigDecimal price = BigDecimal.valueOf(100 + random.nextInt(100));
            BigDecimal change = BigDecimal.valueOf(random.nextInt(2000) - 1000, 2);
            quotes.add(new Quote(stock, price, change));
        }
    }

    /**
     * Императивный цикл: ручной счётчик суммы и количества, без промежуточных объектов конвейера.
     */
    @Benchmark
    public double forEachLoop() {
        double sum = 0.0;
        int count = 0;
        for (Quote quote : quotes) {
            sum += quote.getChangePercent().doubleValue();
            count++;
        }
        return count == 0 ? 0.0 : sum / count;
    }

    @Benchmark
    public double sequentialStream() {
        return quotes.stream()
                .mapToDouble(quote -> quote.getChangePercent().doubleValue())
                .average()
                .orElse(0.0);
    }

    @Benchmark
    public double parallelStream() {
        return quotes.parallelStream()
                .mapToDouble(quote -> quote.getChangePercent().doubleValue())
                .average()
                .orElse(0.0);
    }
}
