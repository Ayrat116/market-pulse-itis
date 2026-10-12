# Spliterator и параллельные стримы

---

## Spliterator — источник стрима

Источник любого стрима — `Spliterator` («splittable iterator»), а не `Iterator`. Помимо последовательного перебора
(`tryAdvance`) он умеет две вещи, которых у `Iterator` нет:

- **делиться** — `trySplit()` отдаёт часть элементов в новый `Spliterator`, оставляя себе остальные;
- **сообщать характеристики** — `SIZED` (известен размер), `ORDERED`, `SORTED`, `DISTINCT`, `IMMUTABLE`.

```java
void example(List<Quote> quotes) {
    Spliterator<Quote> spliterator = quotes.spliterator();
    System.out.println(spliterator.estimateSize());                        // известный размер: SIZED
    System.out.println(spliterator.hasCharacteristics(Spliterator.SORTED)); // false — список не отсортирован

    Spliterator<Quote> left = spliterator.trySplit();   // делит исходный спилитератор надвое "на месте"
}
```

На способности делиться держится параллелизм; характеристики определяют, насколько дёшево и корректно это делать.

---

## Как работает parallelStream()

`collection.parallelStream()` (или `.stream().parallel()`) рекурсивно раскалывает источник через `trySplit`, раздаёт
куски в общий `ForkJoinPool` (по числу ядер), обрабатывает независимо и сливает частичные результаты.

- `ArrayList`, массив — `SIZED`, делятся ровно пополам за `O(1)`: параллелизм ложится хорошо.
- `LinkedList`, `HashSet` — делятся плохо или неравномерно: выигрыш меньше.

---

## Когда параллелизм помогает и когда вредит

Помогает: большой объём данных, независимые и достаточно тяжёлые операции на элемент, источник, который дёшево
делится. Вредит:

- **малый объём** — накладные на split и merge превышают выигрыш;
- **дешёвые операции** — на копеечной операции параллелизм не окупается;
- **боксинг и плохо делящиеся источники**;
- **зависимость от порядка** (`ORDERED`-стримы дороже параллелить);
- **разделяемое изменяемое состояние** — гонки и неверный результат.

Отдельная ловушка — коллектор:

```java
void example(List<Quote> quotes) {
    // groupingBy в параллельном стриме: каждый под-таск строит свою Map, затем они сливаются — лишняя работа
    Map<InstrumentType, List<Quote>> byTypeMerged = quotes.parallelStream()
            .collect(Collectors.groupingBy(quote -> quote.getInstrument().getType()));

    // groupingByConcurrent: одна ConcurrentHashMap на всех, без слияния; порядок внутри групп не гарантирован
    ConcurrentMap<InstrumentType, List<Quote>> byTypeConcurrent = quotes.parallelStream()
            .collect(Collectors.groupingByConcurrent(quote -> quote.getInstrument().getType()));
}
```

---

## Реальный замер: StreamParallelBenchmark

Один и тот же конвейер (`map(Quote::getChangePercent).mapToDouble(BigDecimal::doubleValue).average()`) на массиве
случайных котировок трёх размеров, JMH, AverageTime, 3 fork × 5 warmup × 5 measurement:

| `size` | `sequential`, мкс/оп | `parallel`, мкс/оп |
|---|---|---|
| 100 | ~0,6 | ~13–20 (в десятки раз медленнее) |
| 10 000 | ~60–65 | ~50–55 (параллель чуть впереди) |
| 1 000 000 | ~6 300–6 600 | ~1 300–1 900 (в 3–5 раз быстрее) |

Точка безубыточности — в районе `size=10 000`, и это не универсальная константа: она зависит от стоимости операции
на элемент, делимости источника и наличия боксинга. Перепрогнать перед занятием —
`org.openjdk.jmh.Main "StreamParallelBenchmark.*"`.

---

## А как насчёт обычного for-each? ForEachVsStreamBenchmark

Третий, самый простой вариант обхода часто выпадает из сравнения «sequential vs parallel» — а зря:
`ForEachVsStreamBenchmark` (`size` 1 000 / 100 000 / 1 000 000) добавляет к тем же двум ещё и обычный `for`-each:

| `size` | `for-each`, мкс/оп | `sequential stream`, мкс/оп | `parallel stream`, мкс/оп |
|---|---:|---:|---:|
| 1 000 | 1,3 | 6,3 (в 5 раз медленнее) | 18,5 (в 15 раз медленнее) |
| 100 000 | 145,3 | 631,5 (в 4,3 раза медленнее) | 155,9 (почти наравне) |
| 1 000 000 | 2 037,0 | 6 385,3 (в 3,1 раза медленнее) | 1 806,8 (на ~11% быстрее) |

`for`-each быстрее последовательного стрима на **всех** размерах, не только на малых — часть разницы здесь не
конвейер стрима сам по себе, а то, что `DoubleStream.sum()`/`average()` по документации вправе использовать более
точное, но более дорогое компенсированное суммирование, а не наивное `sum += x` из цикла. Параллельный стрим
обгоняет `for`-each только на `N=1 000 000`, и то скромно (~11%) — полные результаты и разбор: материал
«Результаты ForEachVsStreamBenchmark».

---

## Правило

Параллелизм — не «ускорение по умолчанию», а инструмент под конкретный профиль нагрузки. Прежде чем ставить
`parallel()`, нужно замерить: на малых и средних объёмах последовательный стрим почти всегда быстрее, а обычный
`for`-each — зачастую быстрее их обоих.
