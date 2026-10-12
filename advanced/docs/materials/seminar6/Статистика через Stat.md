# Статистика через Stat — расширяемость команды stats

---

## Та же идея, что у InstrumentFilter

Новый вид отбора в `list` добавлялся без изменения существующего кода: пишется класс, реализующий
`InstrumentFilter` (`boolean matches(Instrument)`), и подключается в одном месте (`FilterFactory`). `ListCommand` при
этом не меняется ни разу.

Статистики в `stats` устроены по тому же принципу — через интерфейс `Stat<S>`:

```java
public interface Stat<S> extends StatCodeAware {
    S calculateStats(List<Quote> quotes);
}
```

- **`StatCodeAware`** — тот же приём, что `InstrumentTypeAware` у `InstrumentFactory`: `getCode()` возвращает
  `StatCode` — подпись статистики в выводе, не завязанную на `getClass().getSimpleName()`.
- **`calculateStats(List<Quote>)`** — один фиксированный аргумент, как `matches(Instrument)` у фильтра. Параметры
  конкретной статистики (если нужны — порог, валюта) передаются через конструктор реализации, а не через аргументы
  метода: метод интерфейса у всех реализаций одинаковый, различается только тело и то, что скрыто в конструкторе.
- **`S`** — тип результата свой у каждой статистики: `BigDecimal` для суммы цен, `Double` для средней доходности,
  `Map<InstrumentType, Long>` для распределения по типу. Там, где у фильтра всегда `boolean`, у статистики — любой
  тип, какой ей самой нужен.

---

## Как добавить новую статистику

1. Написать класс в `stat.impl`, реализующий `Stat<S>` с конкретным `S`:

```java
public class TotalPriceStat implements Stat<BigDecimal> {

    @Getter
    private final StatCode code = StatCode.TOTAL_PRICE;

    @Override
    public BigDecimal calculateStats(List<Quote> quotes) {
        return quotes.stream()
                .map(Quote::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

2. Добавить новое значение в `dictionary.StatCode`.
3. Дописать экземпляр в список `Main.stats()`.

`StatsCommand` не меняется ни на одном из этих шагов — он просто обходит список `List<Stat<?>>` и печатает
`stat.getCode() + ": " + stat.calculateStats(quotes)` для каждого элемента.

```java
// StatsCommand.call()
for (Stat<?> stat : stats) {
    System.out.println(stat.getCode() + ": " + stat.calculateStats(quotes));
}
```

---

## Зарегистрированные статистики

| Код | Результат | Коллектор/операция |
|---|---|---|
| `COUNT_BY_TYPE` | `Map<InstrumentType, Long>` | `groupingBy` + `counting` |
| `COUNT_BY_CURRENCY` | `Map<Currency, Long>` | `groupingBy` + `counting` |
| `AVERAGE_CHANGE` | `Double` | `mapToDouble` + `average` |
| `TOTAL_PRICE` | `BigDecimal` | `reduce` |
| `GAIN_LOSS_SPLIT` | `Map<Boolean, Long>` | `partitioningBy` + `counting` |

> **Для семинариста:** `FilterFactory` выбирает ровно одну реализацию по критерию из CLI (взаимоисключающие
> варианты); `Main.stats()` собирает список статистик, которые печатаются *все сразу* (накопительный отчёт, а не
> выбор одного варианта) — разница в том, как расширение используется, а не в том, как оно устроено.
