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

## Как создать стрим

| Способ | Пример |
|---|---|
| Из коллекции | `list.stream()` |
| Из массива | `Arrays.stream(arr)` / `Arrays.stream(arr, from, to)` — диапазон индексов |
| Фабрики | `Stream.of("X", "Y")`, `Stream.empty()`, `Stream.<String>builder().add("one").add("two").build()` |
| `generate`/`iterate` | бесконечные источники — см. раздел ниже |
| Библиотеки (`java.nio.file.Files`) | `Files.lines(path)` — ленивая построчная обработка без загрузки всего файла в память |

```java
void example(Path path) throws IOException {
    // try-with-resources обязателен: Files.lines держит открытый файловый дескриптор,
    // пока стрим не закрыт (или не обойдён целиком терминальной операцией)
    try (Stream<String> lines = Files.lines(path)) {
        long nonBlank = lines.filter(line -> !line.isBlank()).count();
    }
}
```

---

## Промежуточные и терминальные операции

- **Промежуточные** возвращают новый стрим и соединяются в цепочку: `filter`, `map`, `sorted`, `limit`, `distinct`.
  Они ленивы — ни один элемент не обрабатывается, пока не вызвана терминальная операция.
- **Терминальные** запускают конвейер и дают результат или побочный эффект: `forEach`, `toList`, `count`, `reduce`,
  `collect`. Стрим одноразовый: повторный вызов терминальной операции на нём бросает `IllegalStateException`.

Короткозамкнутые операции (`limit`, `findFirst`, `anyMatch`) останавливают обработку, как только результат определён,
не дожидаясь конца источника — это же свойство делает возможными бесконечные стримы (`Stream.iterate`).

| Операция | Принимает | Что делает |
|---|---|---|
| `peek` | `Consumer<T>` | побочный эффект на каждый элемент, сам элемент не меняет (отладка/логирование — не замена `map`) |
| `distinct` | — | убирает дубликаты (`equals`/`hashCode`) |
| `skip(n)` / `limit(n)` | — | пропустить первые `n` / взять не больше `n` |
| `flatMap` | `Function<T, Stream<R>>` | разворачивает «стрим стримов» в один плоский стрим |
| `sorted` | `Comparator<T>` (или natural order) | сортирует |
| `findFirst` / `findAny` | — | первый/любой подошедший элемент после `filter`, `Optional<T>` |
| `count` | — | сколько элементов в стриме |
| `max` / `min` | `Comparator<T>` | максимальный/минимальный элемент, `Optional<T>` |
| `anyMatch` / `allMatch` / `noneMatch` | `Predicate<T>` | удовлетворяют ли элементы условию (хотя бы один / все / ни один) |

```java
void example(List<Quote> quotes) {
    boolean hasAnyLoser = quotes.stream()
            .anyMatch(quote -> quote.getChangePercent().signum() < 0);   // остановится на первом же падении

    Optional<Quote> firstGainer = quotes.stream()
            .filter(quote -> quote.getChangePercent().signum() > 0)
            .findFirst();

    Optional<Quote> topGainer = quotes.stream()
            .max(Comparator.comparing(Quote::getChangePercent));
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

## Stream.generate и Stream.iterate — бесконечные источники

До сих пор источником была готовая коллекция — конечная по определению. `Stream.generate`/`Stream.iterate` создают
источник, у которого конца нет вовсе: элементы вычисляются по требованию, лениво, один за другим. Без явной границы
конвейер не остановится сам.

| Форма | Элементы | Остановка |
|---|---|---|
| `iterate(seed, next)` | каждый следующий считается из предыдущего | только `limit`/`takeWhile` — забыли границу, конвейер зависнет |
| `iterate(seed, hasNext, next)` (JDK 9+) | то же, но с условием прямо в источнике | встроена — читается как классический `for` |
| `generate(supplier)` | каждый — независимый вызов `supplier`, элементы друг с другом не связаны | только `limit`/`takeWhile` |

```java
void example() {
    // iterate(seed, next): бесконечно, граница — отдельный limit
    List<Integer> powersOfTwo = Stream.iterate(1, n -> n * 2).limit(6).toList();

    // iterate(seed, hasNext, next): условие остановки в самом источнике — как for (int n = 1; n <= 10; n += 3)
    List<Integer> sequence = Stream.iterate(1, n -> n <= 10, n -> n + 3).toList();

    // generate(supplier): элементы независимы — например, синтетические данные для нагрузочного теста
    List<Quote> synthetic = Stream.generate(() -> randomQuote()).limit(1000).toList();
}
```

### Задача: постраничный перебор внешнего сервиса

Реальный HTTP API (в том числе будущий T-Invest API, семинар 7) отдаёт данные страницами: неизвестно заранее,
сколько их будет — конец определяется по ответу (пустая страница), а не по счётчику. Это ровно `iterate` + `takeWhile`,
а не 3-аргументный `iterate` — `hasNext` нужно было бы знать *до* похода за следующей страницей, а узнать это можно
только сходив за ней:

```java
void example() {
    List<Quote> all = Stream.iterate(0, page -> page + 1)       // номера страниц, бесконечно
            .map(this::fetchPage)                                // поход за каждой страницей
            .takeWhile(page -> !page.isEmpty())                  // стоп на первой пустой — короткое замыкание
            .flatMap(List::stream)                               // страницы (список списков) -> плоский стрим
            .toList();
}
```

Полный рабочий пример (эмуляция сервера, отдающего страницы по 2 элемента) — `demo/StreamGeneratorsDemo.java`,
метод `fetchAllPages()`.

> **Вопрос группе.** Почему здесь нельзя просто сделать `Stream.generate(() -> fetchNextPage())`? (Подсказка: что
> `generate` знает о предыдущем вызове `supplier`, и нужно ли это знать, чтобы понять, какую страницу запрашивать
> следующей.)

---

## Что ещё важно знать

1. **Элементы проходят этапы по одному, до конца конвейера** — а не «сначала все `filter`, потом все `map`»: для
   каждого элемента выполняются все операции подряд, прежде чем конвейер перейдёт к следующему элементу. Отсюда и
   работают короткозамкнутые операции (`limit`, `findFirst`) — не нужно гонять через весь конвейер элементы, которые
   уже не понадобятся.
2. **Стримы Lazy (ленивые)** — ничего не выполняется, если нет терминальной операции. `quotes.stream().filter(...)`
   без `.toList()`/`.forEach(...)` в конце — просто описание, не выполнение.
3. **Стримы для примитивов** — `IntStream`/`LongStream`/`DoubleStream` не используют боксинг, поэтому быстрее, чем
   `Stream<Integer>` на тех же данных (см. материал «Spliterator и параллельные стримы» — боксинг там же в списке
   причин, почему параллелизм может не окупаться).
4. **С Java 11 есть `.toList()`** как терминальная операция — короче `collect(Collectors.toList())` и сразу
   неизменяемый список (в отличие от `Collectors.toList()`, который ничего не гарантирует про изменяемость
   результата).
5. **Стримы одноразовые** — переиспользовать тот же стрим после терминальной операции нельзя
   (`IllegalStateException`).
6. **`collect` заслуживает отдельного изучения** — в этом материале разобрана лишь часть `Collectors`; `collect`
   принимает и произвольный, собственный `Collector` (три части: поставщик контейнера, накопление элемента, слияние
   контейнеров — тот же принцип, что у `Gatherer`, но для терминальной операции, см. материал «Gatherer API»).

## Оговорки

- Стрим не меняет источник.
- Промежуточные операции должны быть чистыми (без побочных эффектов) — условие корректной параллельной обработки;
  изменяемое внешнее состояние, которое пишут несколько потоков одновременно на `parallelStream()`, — гонка.
- `peek` — для отладки конвейера, не замена `map`/`forEach`: JDK не гарантирует его вызов для каждого элемента, если
  следующая операция короткозамкнутая.
