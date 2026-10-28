package academy.backend.market_pulse.demo;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.spliterator.ArraySpliterator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

/**
 * Дефолтный {@code Spliterator} списка рядом с самописным {@link ArraySpliterator}.
 * {@code StreamParallelBenchmark} и {@code ForEachVsStreamBenchmark} уже используют параллелизм
 * через встроенный спилитератор {@code ArrayList} неявно (внутри {@code parallelStream()}) — здесь
 * оба показаны явно, напрямую вызванными методами интерфейса.
 */
public class SpliteratorDemo {

    public static void main(String[] args) {
        List<Quote> quotes = sampleQuotes();

        System.out.println("=============== встроенный Spliterator (ArrayList) ===============");
        builtIn(quotes);

        System.out.println("=============== самописный ArraySpliterator ===============");
        custom(quotes);
    }

    /**
     * Встроенный {@code Spliterator} уже умеет сообщать размер и характеристики и однажды
     * корректно делится — {@code ArrayList} делится дёшево и ровно пополам.
     */
    private static void builtIn(List<Quote> quotes) {
        Spliterator<Quote> spliterator = quotes.spliterator();
        System.out.println("  estimateSize() = " + spliterator.estimateSize());
        System.out.println("  hasCharacteristics(SIZED) = " + spliterator.hasCharacteristics(Spliterator.SIZED));
        System.out.println("  hasCharacteristics(SORTED) = " + spliterator.hasCharacteristics(Spliterator.SORTED));

        Spliterator<Quote> left = spliterator.trySplit();
        System.out.println("  после trySplit(): левая половина ~" + left.estimateSize()
                + ", оставшаяся правая ~" + spliterator.estimateSize());
    }

    /**
     * Тот же набор операций над {@link ArraySpliterator} — поведение (деление пополам, точный
     * размер) идентично встроенному, потому что реализован по тому же принципу (диапазон индексов
     * с произвольным доступом). Дальше — {@code StreamSupport.stream(spliterator, true)}: ровно
     * так и собирается параллельный стрим из произвольного {@code Spliterator}, не только из
     * {@code Collection.parallelStream()}.
     */
    private static void custom(List<Quote> quotes) {
        ArraySpliterator<Quote> spliterator = new ArraySpliterator<>(quotes);
        System.out.println("  estimateSize() = " + spliterator.estimateSize());

        Spliterator<Quote> left = spliterator.trySplit();
        System.out.println("  после trySplit(): левая половина ~" + left.estimateSize()
                + ", оставшаяся правая ~" + spliterator.estimateSize());

        double averageViaCustomSpliterator = StreamSupport.stream(new ArraySpliterator<>(quotes), true)
                .mapToDouble(quote -> quote.getChangePercent().doubleValue())
                .average()
                .orElse(0.0);
        double averageViaBuiltIn = quotes.parallelStream()
                .mapToDouble(quote -> quote.getChangePercent().doubleValue())
                .average()
                .orElse(0.0);
        System.out.println("  среднее через StreamSupport.stream(ArraySpliterator, true) = "
                + averageViaCustomSpliterator);
        System.out.println("  среднее через quotes.parallelStream()                     = " + averageViaBuiltIn);
    }

    private static List<Quote> sampleQuotes() {
        Instrument sber = InstrumentFactories.create(InstrumentType.STOCK, "SBER", "Сбербанк", Currency.RUB);
        Instrument gazp = InstrumentFactories.create(InstrumentType.STOCK, "GAZP", "Газпром", Currency.RUB);
        Instrument lkoh = InstrumentFactories.create(InstrumentType.STOCK, "LKOH", "Лукойл", Currency.RUB);
        return List.of(
                new Quote(sber, new BigDecimal("250.00"), new BigDecimal("1.20")),
                new Quote(gazp, new BigDecimal("120.50"), new BigDecimal("-0.80")),
                new Quote(lkoh, new BigDecimal("7100.00"), new BigDecimal("2.30")),
                new Quote(sber, new BigDecimal("251.00"), new BigDecimal("0.40")),
                new Quote(gazp, new BigDecimal("119.80"), new BigDecimal("-0.58")));
    }
}
