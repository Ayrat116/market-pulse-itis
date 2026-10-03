package academy.backend.market_pulse.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code OutOfMemoryError} — наследник {@code Error}, а не {@code Exception}, но синтаксис
 * {@code catch} не различает эти две ветки {@code Throwable} — поймать можно и то, и другое.
 * Демонстрация существует не как образец для подражания: ловить {@code OutOfMemoryError} в
 * продакшен-коде — плохая идея, к моменту его выброса JVM уже может быть в непредсказуемом
 * состоянии (не хватает памяти даже на аккуратную обработку). Здесь это только демонстрация
 * границы языка.
 *
 * <p>Запускать с маленькой кучей, иначе исчерпание займёт заметное время и много мусора:
 * {@code -Xmx128m}.
 */
public class OutOfMemoryDemo {

    public static void main(String[] args) {
        try {
            exhaustHeap();
        } catch (OutOfMemoryError e) {
            System.out.println("Поймали: " + e);
        }
    }

    private static void exhaustHeap() {
        List<byte[]> allocations = new ArrayList<>();
        while (true) {
            // Крупные куски, а не много мелких: меньше итераций и давления на GC до фактического OOM.
            allocations.add(new byte[Integer.MAX_VALUE / 8]);
        }
    }
}
