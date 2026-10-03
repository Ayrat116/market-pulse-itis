package academy.backend.market_pulse.demo;

/**
 * Анти-паттерн: исключения вместо {@code goto}. Здесь нет ни одной настоящей ошибки — каждое
 * исключение существует только ради передачи управления между шагами. Код намеренно запутан:
 * реальный порядок выполнения не совпадает с порядком объявления методов и не читается сверху
 * вниз — разобраться можно только вручную трассируя, какой {@code catch} куда передаёт
 * управление. Задание группе: не запуская код, предсказать порядок строк в выводе; затем
 * запустить и свериться. Мораль — не про то, что тут написан плохой алгоритм, а про то, что
 * исключения для потока управления (а не для сигнала об ошибке) читаются на порядок хуже, чем
 * обычные {@code if}/{@code while}/{@code break}.
 */
public class GoToOnExceptionsDemo {

    private static class JumpToStepB extends RuntimeException {}

    private static class JumpToStepD extends RuntimeException {}

    private static class JumpBackToStepA extends RuntimeException {}

    private static class Done extends RuntimeException {}

    private static int attempt = 0;

    public static void main(String[] args) {
        try {
            stepA();
        } catch (Done ignored) {
            System.out.println("Готово.");
        }
    }

    private static void stepA() {
        System.out.println("Шаг A");
        attempt++;
        try {
            if (attempt < 2) {
                throw new JumpToStepB();
            }
            throw new JumpToStepD();
        } catch (JumpToStepB e) {
            stepC();
        } catch (JumpToStepD e) {
            stepD();
        }
    }

    private static void stepB() {
        System.out.println("Шаг B");
        throw new JumpBackToStepA();
    }

    private static void stepC() {
        System.out.println("Шаг C");
        try {
            stepB();
        } catch (JumpBackToStepA e) {
            stepA();
        }
    }

    private static void stepD() {
        System.out.println("Шаг D");
        throw new Done();
    }
}
