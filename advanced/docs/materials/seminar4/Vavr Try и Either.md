# Vavr: Try и Either

---

## Зачем ещё одна библиотека

[Vavr](https://vavr.io/) — библиотека функциональных типов для Java: неизменяемые коллекции, `Option`, `Try`,
`Either` и другие конструкции, отсутствующие в стандартной библиотеке.

```xml

<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr</artifactId>
    <version>1.0.1</version>
</dependency>
```

---

## Try<T>

Оборачивает вычисление, способное бросить исключение, в значение — `Success(T)` либо `Failure(Throwable)`, без
`try/catch` в месте использования:

```java
void example() {
    Try<Currency> currency = Try.of(() -> Currency.valueOf(currencyString));

    currency.onSuccess(value -> System.out.println("Валюта распознана: " + value))
            .onFailure(error -> System.out.println("Некорректная валюта: " + currencyString));
}
```

---

## Either<L, R>

Представляет одно из двух значений — по соглашению `Left` для ошибки, `Right` для успеха. В отличие от исключения, тип
возможной ошибки виден прямо в сигнатуре метода:

```java
public Either<String, Instrument> parseInstrument(InstrumentType type, String ticker, String name, String currencyStr) {
    try {
        Currency currency = Currency.valueOf(currencyStr.toUpperCase());
        return Either.right(InstrumentFactories.create(type, ticker, name, currency));
    } catch (IllegalArgumentException | InvalidInstrumentException e) {
        return Either.left("Некорректный инструмент: " + e.getMessage());
    }
}
```

```java
void example() {
    parseInstrument(type, ticker, name, currencyStr)
            .peek(instrument -> repository.add(instrument))
            .peekLeft(error -> System.out.println("Ошибка: " + error));
}
```

---

## Когда Try и Either, а когда исключения

Это не замена исключениям, а дополнительный инструмент.

- **`Try`** удобен, когда нужно собрать цепочку операций, каждая из которых может упасть (`map`, `recover`,
  `getOrElseThrow`), или когда код с checked-исключениями приходится вызывать из лямбды, где `throws` недоступен.
- **`Either`** уместен, когда ошибка — ожидаемый исход и часть контракта метода: тип ошибки виден в сигнатуре, и
  вызывающий код не может его не заметить.
- **Исключения** остаются инструментом для действительно исключительных ситуаций и для границ API, где принято их
  бросать.

С точки зрения производительности `Try` ничего не экономит: внутри он перехватывает обычное исключение и платит ту же
цену `fillInStackTrace()`, что и `try/catch` (замеры — в «Результаты JMH»). Его преимущество — выразительность
композиции, а не скорость.
