# Stream API

---

## Что такое стрим

Stream — конвейер обработки последовательности элементов, а не структура данных: не хранит данные и не изменяет
источник, из которого создан. `instruments.stream().sorted()` не сортирует сам список, а строит новый конвейер.
Stream API позволяет выразить обработку декларативно — *что* нужно получить, а не *как* это пошагово накапливать.

```java
void example(List<Instrument> instruments) {
    List<String> tickers = instruments.stream()
            .filter(i -> i.getCurrency() == Currency.RUB)
            .map(Instrument::getTicker)
            .sorted()
            .toList();
}
```

Источник стрима — `Spliterator`, а не `Iterator`: он умеет не только перебирать элементы, но и сообщать их размер и
порядок, а также делиться на части. На этой способности делиться держатся параллельные стримы — подробнее в
материале «Spliterator и параллельные стримы».

---

## Промежуточные и терминальные операции

- **Промежуточные** возвращают новый стрим и соединяются в цепочку: `filter`, `map`, `sorted`, `limit`, `distinct`.
  Они ленивы — ни один элемент не обрабатывается, пока не вызвана терминальная операция.
- **Терминальные** запускают конвейер и дают результат или побочный эффект: `forEach`, `toList`, `count`, `reduce`,
  `collect`. Стрим одноразовый: повторный вызов терминальной операции на нём бросает `IllegalStateException`.

Короткозамкнутые операции (`limit`, `findFirst`, `anyMatch`) останавливают обработку, как только результат определён,
не дожидаясь конца источника — это же свойство делает возможными бесконечные стримы (`Stream.iterate`).

```java
void example(List<Quote> quotes) {
    boolean hasAnyLoser = quotes.stream()
            .anyMatch(quote -> quote.getChangePercent().signum() < 0);   // остановится на первом же падении
}
```

---

## reduce — свёртка без коллектора

`reduce` сворачивает стрим в одно значение через ассоциативную операцию. Две основные формы:

```java
void example(List<Quote> quotes) {
    // identity + накопитель: результат гарантирован даже для пустого стрима
    BigDecimal totalPrice = quotes.stream()
            .map(Quote::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // без identity: Optional<T> — результата может не быть
    Optional<Quote> anyLoser = quotes.stream()
            .reduce((a, b) -> a.getChangePercent().signum() < 0 ? a : b);
}
```

Для денег — именно `reduce` над `BigDecimal`, а не `mapToDouble().sum()`: перевод в `double` теряет точность, для
отображаемого процента (как в `AverageChangeStat`) это приемлемо, для суммы цен (`TotalPriceStat`) — нет.

---

## collect и Collectors

`reduce` подходит, когда накопитель — неизменяемое значение. Когда накопитель — изменяемая структура (`List`, `Map`),
используется `collect(Collector)`: `java.util.stream.Collectors` — фабрика готовых коллекторов.

| Коллектор | Результат |
|---|---|
| `toList()` / `toSet()` | `List<T>` / `Set<T>` |
| `groupingBy(classifier)` | `Map<K, List<T>>` |
| `groupingBy(classifier, downstream)` | `Map<K, R>` — группа дополнительно схлопывается вторым коллектором |
| `partitioningBy(predicate)` | `Map<Boolean, List<T>>` — всегда ровно два ключа |
| `partitioningBy(predicate, downstream)` | то же, но каждая половина — не список, а результат `downstream` |
| `toMap(keyFn, valueFn, mergeFn)` | `Map<K,V>`; `mergeFn` разрешает дубликаты ключа |
| `joining(delimiter, prefix, suffix)` | конкатенация строк |

```java
void example(List<Quote> quotes) {
    // groupingBy + counting: количество котировок по типу инструмента (CountByTypeStat)
    Map<InstrumentType, Long> byType = quotes.stream()
            .collect(Collectors.groupingBy(
                    quote -> quote.getInstrument().getType(),
                    Collectors.counting()));

    // partitioningBy + counting: выросло/не выросло, сразу счётчиком, а не списком (GainLossSplitStat)
    Map<Boolean, Long> gainLossCount = quotes.stream()
            .collect(Collectors.partitioningBy(
                    quote -> quote.getChangePercent().signum() > 0,
                    Collectors.counting()));
}
```

---

## flatMap и Optional.stream()

`flatMap` разворачивает «стрим стримов» в один стрим. Частая идиома — отбросить отсутствующие значения без проверок
на `null`: `Optional.stream()` даёт пустой стрим для `Optional.empty()` и одноэлементный — для присутствующего
значения (так устроена команда `compare`):

```java
void example(List<String> tickers, QuoteService quoteService) {
    List<Quote> found = tickers.stream()
            .map(quoteService::quoteFor)   // String -> Optional<Quote>
            .flatMap(Optional::stream)
            .toList();
}
```

---

## Оговорки

- Стрим не меняет источник.
- Промежуточные операции должны быть чистыми (без побочных эффектов) — условие корректной параллельной обработки;
  изменяемое внешнее состояние, которое пишут несколько потоков одновременно на `parallelStream()`, — гонка.
- `peek` — для отладки конвейера, не замена `map`/`forEach`: JDK не гарантирует его вызов для каждого элемента, если
  следующая операция короткозамкнутая.
