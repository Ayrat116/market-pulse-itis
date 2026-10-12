# Gatherer API

---

## Зачем

Набор промежуточных операций стрима фиксирован: `map`, `filter`, `limit`, `distinct`, `sorted`. Своей промежуточной
операции с состоянием — например, «скользящее окно» — до недавнего времени написать было нельзя, приходилось выходить
из стрима в цикл. Gatherer API (JEP 461/485, стабилизирован в Java 24) закрывает этот пробел: `Stream.gather(Gatherer)`
— расширяемая промежуточная операция, ровно как `collect(Collector)` — расширяемая терминальная.

---

## Устройство Gatherer

Gatherer описывается четырьмя частями (нужные — переопределяются, остальные опускаются):

- **initializer** — создаёт изменяемое состояние (например, окно последних `k` значений);
- **integrator** — обрабатывает очередной элемент: читает и обновляет состояние, может испустить элементы вниз по
  конвейеру через `downstream.push(...)` и досрочно завершить обработку, вернув `false`;
- **combiner** — сливает состояния двух частей при параллельном выполнении (для последовательных gatherer не нужен);
- **finisher** — по окончании входа испускает «хвост» (например, незакрытый остаток).

Ключевое отличие от `map`/`filter` — состояние между элементами; отличие от `Collector` — результат остаётся стримом,
конвейер продолжается.

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

    // scan: "растущий итог" цены вместо одной суммы
    List<BigDecimal> runningTotal = prices.stream()
            .gather(Gatherers.scan(() -> BigDecimal.ZERO, BigDecimal::add))
            .toList();
}
```

---

## Кастомный gatherer: скользящее среднее (MovingAverageGatherer)

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
