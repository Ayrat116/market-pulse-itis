package academy.backend.market_pulse.spliterator;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Простейший самописный {@link Spliterator} над {@link List} с произвольным доступом (семинар 6):
 * хранит не сам список, а диапазон индексов {@code [from, to)} в нём — без копирования элементов.
 * Показывает контракт на минимальном примере, без претензии на универсальность (в отличие от
 * встроенного спилитератора {@code ArrayList}, не умеет {@code ORDERED}-перенастройку середины
 * диапазона и прочие детали промышленной реализации).
 *
 * <p>Используется так же, как встроенный: {@code StreamSupport.stream(spliterator, parallel)}
 * (см. {@code demo/SpliteratorDemo.java}).
 */
public class ArraySpliterator<T> implements Spliterator<T> {

    private final List<T> source;
    private int from;
    private final int to;

    public ArraySpliterator(List<T> source) {
        this(source, 0, source.size());
    }

    private ArraySpliterator(List<T> source, int from, int to) {
        this.source = source;
        this.from = from;
        this.to = to;
    }

    /** Перебор: отдать текущий элемент и сдвинуться на один вперёд; {@code false} — диапазон исчерпан. */
    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        // NOTICE: аналог hasNext()+next() итератора — если следующий элемент есть (from < to),
        // применить к нему action, сдвинуть from и вернуть true; если диапазон исчерпан —
        // false, без исключений.
        // TODO: семинар 6, этап 4 — tryAdvance
        throw new UnsupportedOperationException("ArraySpliterator.tryAdvance ещё не реализован");
    }

    /**
     * Делит диапазон пополам: себе оставляет вторую половину, в новый {@link Spliterator} отдаёт
     * первую — именно этот вызов (рекурсивно) раскалывает работу между потоками {@code ForkJoinPool}
     * у {@code parallelStream()}. {@code null} — делить больше не имеет смысла (меньше двух
     * элементов осталось), дальше — последовательный перебор через {@link #tryAdvance}.
     */
    @Override
    public Spliterator<T> trySplit() {
        // NOTICE: делит диапазон пополам; себе оставляет ВТОРУЮ половину (сдвигает свой from), в
        // новый Spliterator отдаёт ПЕРВУЮ (from исходный, to = splitPoint). null, если осталось
        // меньше двух элементов — делить дальше не имеет смысла.
        // TODO: семинар 6, этап 4 — trySplit
        throw new UnsupportedOperationException("ArraySpliterator.trySplit ещё не реализован");
    }

    /** Размер известен точно (диапазон индексов) — отсюда характеристика {@code SIZED}. */
    @Override
    public long estimateSize() {
        return to - from;
    }

    /**
     * {@code ORDERED} — порядок элементов в {@code source} значим и сохраняется;
     * {@code SIZED}/{@code SUBSIZED} — размер точно известен и остаётся точно известным после
     * любого {@code trySplit} (оба куска — тоже диапазоны индексов); {@code IMMUTABLE} — на время
     * обхода предполагаем, что {@code source} не меняется извне (упрощение для этого примера, не
     * проверяется рантаймом).
     */
    @Override
    public int characteristics() {
        return ORDERED | SIZED | SUBSIZED | IMMUTABLE;
    }
}
