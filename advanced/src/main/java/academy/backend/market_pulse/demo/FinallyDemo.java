package academy.backend.market_pulse.demo;

import java.io.IOException;
import java.util.function.Function;

/**
 * Две стороны одной монеты — что происходит с исключением, когда ресурс закрывается вручную
 * ({@code finally}) и когда автоматически (try-with-resources).
 *
 * <p>С {@code finally}: {@code return} или новое исключение в {@code finally} «проглатывает»
 * исходное исключение (или {@code return}) из {@code try} — компилятор это молча разрешает, и
 * исходная ошибка теряется безвозвратно, без единого следа.
 *
 * <p>С try-with-resources: исключение из {@code close()} не теряется, а добавляется к основному
 * как <em>подавленное</em> (suppressed) — доступно через {@link Throwable#getSuppressed()}.
 */
public class FinallyDemo {

    public static void main(String[] args) {
        System.out.println("1. Порядок вызовов в finally");
        finallyOrder();
        System.out.println();

        System.out.println("2. return в finally проглатывает исключение из try:");
        System.out.println("Результат: " + returnInFinallySwallowsException());

        System.out.println();
        System.out.println("3. Исключение из finally проглатывает исключение из try:");
        try {
            exceptionInFinallySwallowsException();
        } catch (RuntimeException e) {
            System.out.println("Поймали: " + e.getMessage() + " — исходное \"Ошибка из try\" потеряно совсем");
        }

        System.out.println();
        System.out.println("4. try-with-resources закрывает ресурс автоматически, даже при исключении в теле:");
        try (ImportSession session = new ImportSession()) {
            session.importLine("STOCK,SBER,Сбербанк,250");
        }

        System.out.println();
        System.out.println("5. Исключение из close() не теряется, как в finally, а становится подавленным:");
        try (FailingImportSession session = new FailingImportSession()) {
            throw new IllegalStateException("Ошибка импорта строки");
        } catch (Exception e) {
            // наружу вылетает IllegalStateException из тела блока — оно основное, возникло первым
            // по хронологии; IOException из close() прикреплён к нему как подавленное, а не потерян
            System.out.println("Основное исключение: " + e);
            for (Throwable suppressed : e.getSuppressed()) {
                System.out.println("Подавленное исключение: " + suppressed);
            }
        }

        Function<Integer, Integer> f = i -> {
            try {
                Thread.sleep(1_000);
                return i;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static int multiplier(int i, int j) throws Exception {
        return i * j;
    }

    /**
     * Порядок исполнения finally.
     */
    private static int finallyOrder() {
        try {
            System.out.println("Before Call");
            return call();
        } finally {
            System.out.println("Finally");
        }
    }

    private static int call() {
        System.out.println("In call");
        return 0;
    }

    private static int returnInFinallySwallowsException() {
        try {
            throw new IllegalStateException("Ошибка из try");
        } finally {
            return 42; // исключение из try теряется без единого следа
        }
    }

    private static void exceptionInFinallySwallowsException() {
        try {
            throw new IllegalStateException("Ошибка из try");
        } finally {
            throw new IllegalStateException("Ошибка из finally"); // полностью заменяет исходное исключение
        }
    }

    /**
     * Ресурс, который закрывается штатно — для контраста с {@link FailingImportSession}.
     */
    private static class ImportSession implements AutoCloseable {

        ImportSession() {
            System.out.println("Сессия импорта открыта");
        }

        void importLine(String line) {
            System.out.println("Импортируем: " + line);
        }

        @Override
        public void close() {
            System.out.println("Сессия импорта закрыта");
        }
    }

    /**
     * Ресурс, чей {@code close()} всегда бросает исключение — нужен только для демонстрации
     * подавленных исключений (suppressed exceptions).
     */
    private static class FailingImportSession implements AutoCloseable {

        void importLine(String line) {
            System.out.println("Импортируем: " + line);
        }

        @Override
        public void close() throws IOException {
            throw new IOException("Не удалось закрыть сессию импорта");
        }
    }
}
