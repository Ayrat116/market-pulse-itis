package academy.backend.market_pulse.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Инвариантность generics в сравнении с ковариантностью массивов, bounded type parameter,
 * upper/lower bounded wildcard и PECS — на общей иерархии {@code Parent → Middle → Child}, без
 * привязки к домену market-pulse: сегодняшняя тема про сам механизм типов, а не про инструменты.
 */
public class GenericsVarianceDemo {

    static class Parent {
        @Override
        public String toString() {
            return "Parent";
        }
    }

    static class Middle extends Parent {
        @Override
        public String toString() {
            return "Middle";
        }
    }

    static class Child extends Middle {
        @Override
        public String toString() {
            return "Child";
        }
    }

    public static void main(String[] args) {
        System.out.println("1. Массивы ковариантны, но без страховки компилятора:");
        arraysAreCovariantButUnsafe();

        System.out.println();
        System.out.println("2. Generics инвариантны — та же ошибка, но уже на этапе компиляции:");
        genericsAreInvariant();

        System.out.println();
        System.out.println("3. Bounded type parameter — только extends, super к нему неприменим:");
        boundedTypeParameter();

        System.out.println();
        System.out.println("4. Upper bounded wildcard (? extends) — читаем, писать почти нельзя:");
        upperBoundedWildcard();

        System.out.println();
        System.out.println("5. Lower bounded wildcard (? super) — пишем, читаем почти ничего:");
        lowerBoundedWildcard();

        System.out.println();
        System.out.println("6. PECS: extends и super нужны одновременно, в одной сигнатуре:");
        pecsInAction();
    }

    /**
     * Массивы в Java ковариантны: {@code Middle[]} можно присвоить переменной {@code Parent[]}.
     * Компилятор это разрешает, потому что теоретически в {@code Parent[]} можно класть только
     * {@code Parent} и его подтипы — но именно это и происходит: кладём честный {@code Parent},
     * который не является {@code Middle}, и JVM ловит несоответствие только в рантайме, когда уже
     * поздно.
     */
    private static void arraysAreCovariantButUnsafe() {
        Middle[] middles = new Middle[3];
        Parent[] parents = middles; // компилируется — массивы ковариантны

        try {
            parents[0] = new Parent();
            System.out.println("Не должны были сюда попасть");
        } catch (ArrayStoreException e) {
            System.out.println("ArrayStoreException в рантайме: " + e.getMessage());
        }
    }

    /**
     * У generics той же дыры нет — но не потому что её починили, а потому что
     * {@code List<Middle>} и {@code List<Parent>} вообще не находятся в отношении подтипирования:
     * инвариантны. Присвоить одно другому — ошибка компиляции, а не рантайма.
     *
     * <p><b>Вопрос группе:</b> а что было бы, если бы следующая строка скомпилировалась? Ответ —
     * {@code parents.add(new Parent())} молча испортил бы {@code middles} тем же самым
     * несоответствием типов, что и с массивом выше, только без единого шанса поймать это ни
     * компилятором, ни рантаймом: {@code List.add} не делает проверку типа элемента, которую
     * делает JVM при записи в массив.
     */
    private static void genericsAreInvariant() {
        List<Middle> middles = new ArrayList<>();
        middles.add(new Middle());

        // List<Parent> parents = middles;
        // ОШИБКА КОМПИЛЯЦИИ: incompatible types
        // Если бы это скомпилировалось — parents.add(new Parent()) молча испортил бы middles.

        System.out.println("List<Middle> нельзя присвоить List<Parent> — ошибка компиляции, не рантайма");
    }

    /**
     * Bounded type parameter ограничивает {@code T} сверху — только {@code extends}.
     * {@code super} к типовому параметру неприменим вовсе: он существует только для wildcard'ов
     * (см. {@link #upperBoundedWildcard()}/{@link #lowerBoundedWildcard()} ниже).
     */
    static class Box<T extends Comparable<T>> {

        private final T value;

        Box(T value) {
            this.value = value;
        }

        int compareTo(Box<T> other) {
            return value.compareTo(other.value);
        }
    }

    // class Box2<T super Middle> {}
    // ОШИБКА КОМПИЛЯЦИИ: super не применим к типовым параметрам, только к wildcard'ам

    private static void boundedTypeParameter() {
        Box<Integer> one = new Box<>(1);
        Box<Integer> two = new Box<>(2);
        System.out.println("Box<Integer>(1).compareTo(Box<Integer>(2)) = " + one.compareTo(two));
    }

    /**
     * {@code ? extends Middle} — upper bounded wildcard: список гарантированно хранит
     * {@code Middle} или его подтип, поэтому читать можно как минимум {@code Middle}. Писать
     * нельзя ничего, кроме {@code null}: компилятор не знает, какой именно подтип {@code Middle}
     * сейчас на самом деле в списке, и не может проверить безопасность записи.
     */
    private static void upperBoundedWildcard() {
        List<? extends Middle> list = List.of(new Middle(), new Child());

        Middle m = list.get(0); // OK — гарантированно хотя бы Middle
        Parent p = list.get(0); // OK — Middle это тоже Parent
        System.out.println("Прочитали как Middle и как Parent: " + m + ", " + p);

        // list.add(new Middle());
        // ОШИБКА КОМПИЛЯЦИИ: неизвестно, точно ли там Middle, а не более узкий подтип
    }

    /**
     * {@code ? super Middle} — lower bounded wildcard: список гарантированно хранит {@code Middle}
     * или один из его супертипов, поэтому писать можно {@code Middle} и его подтипы — они точно
     * впишутся в любой супертип {@code Middle}. Читать при этом можно только как {@code Object}:
     * компилятор не знает, насколько широкий супертип {@code Middle} сейчас используется.
     */
    private static void lowerBoundedWildcard() {
        List<? super Middle> list = new ArrayList<>(List.of(new Parent()));

        list.add(new Middle()); // OK
        list.add(new Child());  // OK — Child это тоже Middle

        // list.add(new Parent());
        // ОШИБКА КОМПИЛЯЦИИ: Parent не гарантированно Middle

        Object o = list.get(0); // единственное, что гарантировано при чтении
        // Middle m = list.get(0); // ОШИБКА КОМПИЛЯЦИИ

        System.out.println("Записали Middle и Child, прочитать можно только как Object: " + o);
    }

    /**
     * PECS (Producer Extends, Consumer Super) в действии — метод копирования, аналог
     * {@code Collections.copy}. Здесь {@code extends} и {@code super} нужны одновременно, в одной
     * сигнатуре: {@code src} только отдаёт элементы (producer → {@code extends}), {@code dest}
     * только принимает их (consumer → {@code super}).
     *
     * <p>Если бы сигнатура была {@code copy(List<T> dest, List<T> src)} без wildcard'ов, вызов
     * ниже не скомпилировался бы: {@code List<Object>} и {@code List<Middle>} не находятся в
     * отношении подтипирования — та же инвариантность, с которой начали этот демо-класс.
     */
    private static <T> void copy(List<? super T> dest, List<? extends T> src, Predicate<? super T> predicate) {
        for (int i = 0; i < src.size(); i++) {
            if (predicate.test(src.get(i))) {
                dest.set(i, src.get(i));
            }
        }
    }

    private static void pecsInAction() {
        List<Middle> middles = List.of(new Middle(), new Child());
        List<Object> destination = new ArrayList<>(List.of(new Object(), new Object()));

        copy(destination, middles, __ -> true); // компилируется и работает
        System.out.println("Скопировали List<Middle> в List<Object>: " + destination);
    }
}
