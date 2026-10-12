package academy.backend.market_pulse.gather;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.stream.Gatherer;

/**
 * Кастомный {@link Gatherer}: разница между максимальным и минимальным значением в скользящем окне
 * из {@code windowSize} подряд идущих цен (семинар 6, Gatherer API — ровно та «реальная задача»,
 * под которую Gatherer API и представлен: промежуточная операция с состоянием, которую
 * {@code map}/{@code filter}/{@code limit} не выразят).
 *
 * <p>Окно хранится в {@link Deque} (состояние {@code integrator}); min/max внутри окна
 * пересчитываются заново на каждый элемент — просто и корректно, но {@code O(windowSize)} на шаг.
 * Для больших окон это можно оптимизировать до амортизированного {@code O(1)} монотонной
 * очередью (как в {@link MovingAverageGatherer}, где сумма окна обновляется инкрементально, а не
 * пересчитывается) — здесь оставлено простым ради читаемости примера.
 */
public final class SlidingSpreadGatherer {

    private SlidingSpreadGatherer() {
    }

    public static Gatherer<BigDecimal, ?, BigDecimal> spread(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Размер окна должен быть положительным");
        }
        return Gatherer.ofSequential(
                State::new,
                (state, price, downstream) -> {
                    state.window.addLast(price);
                    if (state.window.size() > windowSize) {
                        state.window.removeFirst();
                    }
                    if (state.window.size() == windowSize) {
                        BigDecimal min = state.window.stream().min(Comparator.naturalOrder()).orElseThrow();
                        BigDecimal max = state.window.stream().max(Comparator.naturalOrder()).orElseThrow();
                        return downstream.push(max.subtract(min));
                    }
                    return true;
                });
    }

    private static final class State {
        private final Deque<BigDecimal> window = new ArrayDeque<>();
    }
}
