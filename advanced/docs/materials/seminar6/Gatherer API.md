# Gatherer API

---

## Зачем

Набор промежуточных операций стрима фиксирован: `map`, `filter`, `limit`, `distinct`, `sorted`. Своей промежуточной
операции с состоянием — например, «скользящее окно» — до недавнего времени написать было нельзя, приходилось выходить
из стрима в цикл. Gatherer API (JEP 461/485, стабилизирован в Java 24) закрывает этот пробел: `Stream.gather(Gatherer)`
— расширяемая промежуточная операция, ровно как `collect(Collector)` — расширяемая терминальная.

---

## Устройство Gatherer

Gatherer описывается четырьмя методами (нужные — переопределяются, остальные опускаются):

1. **`initializer()`** — создаёт новое состояние `A` для каждой последовательной секции (например, окно последних
   `k` значений).
2. **`integrator(state, element, downstream)`** — обрабатывает элемент: обновляет `state`, при необходимости
   запускает `downstream` (испускает элемент вниз по конвейеру). Возвращает `boolean` — ровно как
   `Spliterator.tryAdvance()` (см. материал «Spliterator и параллельные стримы»): `true` — продолжать, `false` —
   завершить обработку досрочно.
3. **`combiner(state1, state2)`** — сливает два состояния (для параллельных стримов), необязателен.
4. **`finisher(state, downstream)`** — завершающий выброс остатков из состояния, необязателен.

Ключевое отличие от `map`/`filter` — состояние между элементами; отличие от `Collector` — результат остаётся стримом,
конвейер продолжается.

---

## Реальная задача: разброс цены в скользящем окне (SlidingSpreadGatherer)

Собрать разницу между максимальным и минимальным значением в скользящем окне из `N` последних цен — классический
пример, под который и создавался Gatherer API: ни `map`, ни `filter`, ни терминальный `reduce` не выражают «помни
последние `N` значений и пересчитывай что-то по ним на каждый новый элемент» — для этого нужна промежуточная
операция с собственным состоянием.

```java
public static Gatherer<BigDecimal, ?, BigDecimal> spread(int windowSize) {
    return Gatherer.ofSequential(
            State::new,                                    // initializer
            (state, price, downstream) -> {                // integrator
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
```

Используется в `history TICKER --spread --sma K` (`HistoryCommand`, `SlidingSpreadGatherer`); тот же пример —
`demo/GathererDemo.java`. Здесь min/max окна пересчитываются заново на каждый элемент (`O(windowSize)` на шаг) —
просто и наглядно; для больших окон это можно оптимизировать до амортизированного `O(1)`, обновляя min/max
инкрементально, как в `MovingAverageGatherer` ниже обновляется сумма.

---

## Встроенные gatherer

Фабрики в `java.util.stream.Gatherers`:

- `windowFixed(n)` — режет поток на неперекрывающиеся окна по `n` элементов;
- `windowSliding(n)` — скользящие окна по `n` подряд идущих элементов;
- `scan(initial, scanner)` — как `reduce`, но испускает каждое промежуточное состояние, а не только финальное;
- `mapConcurrent(n, mapper)` — параллельное отображение с ограничением конкурентности `n`.

```java
void example(List<BigDecimal> prices) {
    List<List<BigDecimal>> windows = prices.stream()
            .gather(Gatherers.windowSliding(3))   // окна по 3 подряд идущих цены
            .toList();

    // windowFixed: НЕперекрывающиеся пакеты — в отличие от windowSliding, соседние пакеты не
    // делят между собой элементы; последний пакет может быть короче n
    List<List<BigDecimal>> batches = prices.stream()
            .gather(Gatherers.windowFixed(2))
            .toList();

    // scan: "растущий итог" цены вместо одной суммы
    List<BigDecimal> runningTotal = prices.stream()
            .gather(Gatherers.scan(() -> BigDecimal.ZERO, BigDecimal::add))
            .toList();

    // mapConcurrent: тот же map, но до n вызовов функции параллельно — для mapper'а, который сам
    // по себе медленный и блокирующий (поход в сеть), а не арифметика на CPU
    List<String> enriched = List.of("SBER", "GAZP", "LKOH").stream()
            .gather(Gatherers.mapConcurrent(3, this::fetchFromSlowExternalService))
            .toList();
}
```

Все четыре фабрики на одних данных, с замером времени у `mapConcurrent` (обычный `map` против `mapConcurrent(3)` на
«медленном» вызове) — `demo/GathererDemo.java`.

---

## Ещё один кастомный gatherer: скользящее среднее (MovingAverageGatherer)

Своя промежуточная операция создаётся через `Gatherer.ofSequential(initializer, integrator)`. Скользящее среднее «на
лету» держит окно последних `k` цен и их сумму, испуская среднее, как только окно заполнилось — в отличие от
`windowSliding`, который сначала материализует списки-окна, такой gatherer обновляет сумму инкрементально и не
хранит все окна в памяти:

```java
public static Gatherer<BigDecimal, ?, BigDecimal> movingAverage(int windowSize) {
    return Gatherer.ofSequential(
            State::new,                                   // initializer
            (state, price, downstream) -> {               // integrator
                state.window.addLast(price);
                state.sum = state.sum.add(price);
                if (state.window.size() > windowSize) {
                    state.sum = state.sum.subtract(state.window.removeFirst());
                }
                if (state.window.size() == windowSize) {
                    return downstream.push(state.average(windowSize));
                }
                return true;
            });
}
```

Используется в `history TICKER --sma K` (`HistoryCommand`, `MovingAverageGatherer`).

> **Для семинариста:** подчеркните, чем Gatherer отличается от `map`/`filter` (состояние между элементами) и от
> `Collector` (остаётся промежуточным — конвейер продолжается).
