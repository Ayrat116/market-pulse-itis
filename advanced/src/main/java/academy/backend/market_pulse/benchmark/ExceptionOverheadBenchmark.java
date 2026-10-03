package academy.backend.market_pulse.benchmark;

import io.vavr.control.Try;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Сравнение стоимости кода возврата, исключения с трассировкой стека и исключения без неё.
 *
 * <p><b>Как запустить:</b>
 * <ol>
 *     <li>Собрать проект — {@code mvn -pl advanced package} (или просто собрать в IDE). Это важно:
 *     аннотации {@code @Benchmark} сами по себе ничего не запускают, их превращает в код
 *     annotation-процессор {@code jmh-generator-annprocess}, который отрабатывает только во время
 *     компиляции и кладёт сгенерированные классы в {@code target/classes} — без свежей сборки
 *     запускать нечего.</li>
 *     <li>Запустить сам класс — {@code main(String[])} ниже просто делегирует в
 *     {@code org.openjdk.jmh.Main}, поэтому подходит любой обычный способ: кнопка Run в IDE на
 *     {@code main}, или из терминала:
 *     {@code java -cp target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) \
 *     academy.backend.market_pulse.benchmark.ExceptionOverheadBenchmark}.</li>
 * </ol>
 * Отдельный JMH-плагин IntelliJ IDEA или ручная сборка jar не нужны — оба варианта тоже работают,
 * но {@code main()} проще и ничего дополнительного не требует.
 *
 * <p>Прогон занимает пару минут: 6 бенчмарков × (5 итераций прогрева + 5 замеров) × 1 секунда, плюс
 * запуск отдельной JVM на каждый форк (у двух последних бенчмарков — свой форк с явным
 * {@code -XX:±OmitStackTraceInFastThrow}, см. NOTICE у {@code implicitExceptionOmitEnabled()}).
 *
 * @see <a href="https://www.baeldung.com/java-microbenchmark-harness">Baeldung: JMH</a>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ExceptionOverheadBenchmark {

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    @Benchmark
    public int returnCode() {
        return isKnown("CRYPTO") ? 0 : 1;
    }

    @Benchmark
    public int throwWithStackTrace() {
        try {
            validateOrThrow("CRYPTO");
            return 0;
        } catch (IllegalArgumentException e) {
            return 1;
        }
    }

    // NOTICE: Try внутри всё равно перехватывает обычное JVM-исключение — то есть платит ту же
    // цену fillInStackTrace(), что и throwWithStackTrace(). Цифры ниже должны это подтвердить:
    // Try — не более дешёвая альтернатива try/catch, а более выразительная композиция.
    @Benchmark
    public int throwWithTry() {
        return Try.run(() -> validateOrThrow("CRYPTO"))
                .map(_ -> 0)
                .recover(IllegalArgumentException.class, 1)
                .get();
    }

    @Benchmark
    public int throwWithoutStackTrace() {
        try {
            validateOrThrowFast("CRYPTO");
            return 0;
        } catch (FastValidationException e) {
            return 1;
        }
    }

    private void validateOrThrow(String type) {
        if (!isKnown(type)) {
            // NOTICE: почему fillInStackTrace - публичный?
            // - Manual Refresh and Reuse
            // - Performance Tuning (Overriding)
            // - Custom Exception Pooling
            throw new IllegalArgumentException("Unknown instrument type: " + type);
        }
    }

    private void validateOrThrowFast(String type) {
        if (!isKnown(type)) {
            throw new FastValidationException("Unknown instrument type: " + type);
        }
    }

    private boolean isKnown(String type) {
        return "STOCK".equals(type) || "BOND".equals(type) || "ETF".equals(type);
    }

    static class FastValidationException extends RuntimeException {
        FastValidationException(String message) {
            super(message, null, false, false); // enableSuppression=false, writableStackTrace=false
        }
    }

    // NOTICE: OmitStackTraceInFastThrow — оптимизация JIT (C2), а не общий механизм для любых
    // исключений. Она касается только пяти implicit-исключений, которые сама JVM порождает при
    // провале байт-код инструкции (NullPointerException, ArithmeticException,
    // ArrayIndexOutOfBoundsException, ArrayStoreException, ClassCastException) — и не касается
    // ничего, что брошено явно через throw new (как throwWithStackTrace() выше). Поэтому здесь —
    // честный ArrayIndexOutOfBoundsException от выхода за границы массива, а не throw new
    // ArrayIndexOutOfBoundsException(...): последнее исключение из-под оптимизации выпадает точно
    // так же, как IllegalArgumentException, и разницы между двумя бенчмарками не покажет.
    private static final int[] EMPTY_ARRAY = new int[0];

    @Benchmark
    @Fork(jvmArgsAppend = "-XX:+OmitStackTraceInFastThrow")
    public int implicitExceptionOmitEnabled() {
        try {
            return EMPTY_ARRAY[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        }
    }

    @Benchmark
    @Fork(jvmArgsAppend = "-XX:-OmitStackTraceInFastThrow")
    public int implicitExceptionOmitDisabled() {
        try {
            return EMPTY_ARRAY[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        }
    }
}
