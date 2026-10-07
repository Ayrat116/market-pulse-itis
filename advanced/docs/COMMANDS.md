# Команды CLI

Запуск: `java -cp <classpath> academy.backend.market_pulse.Main <команда> [аргументы]`.

Стартовые данные (создаются при каждом запуске заново, ничего не сохраняется между запусками,
кроме `save` в файл): `SBER` (акция, RUB), `OFZ26233` (облигация, RUB), `FXUS` (ETF, USD).

## add

Добавить инструмент в репозиторий.

```
add <TYPE> <TICKER> <NAME> <CURRENCY>
```

- `TYPE` — `STOCK`, `BOND` или `ETF`
- `CURRENCY` — `RUB`, `USD` или `EUR`

```bash
add STOCK LKOH Лукойл RUB
```

```
Добавлено: Акция, сектор: Unspecified
```

Повторное добавление уже существующего тикера — ошибка (делистинг не выполняется автоматически).

## list

Показать инструменты с опциональной фильтрацией и сортировкой.

```
list [--type TYPE] [--ticker SUBSTRING] [--currency CURRENCY]
     [--price-op GE|LE|EQ --price VALUE] [--sort-by TICKER|CURRENCY|PRICE] [--order ASC|DESC]
```

```bash
list
list --type STOCK
list --currency RUB --sort-by TICKER
list --price-op GE --price 6.0 --sort-by PRICE --order DESC
```

```
Акция, сектор: Финансы
```

`--price-op`/`--price` фильтруют только акции — по дивидендной доходности (`Stock.getDividendYield()`);
облигации и ETF под этот фильтр не попадают никогда, независимо от значения.

## watch

Добавить тикер в watchlist или показать текущий список (без аргумента).

```
watch [TICKER]
```

```bash
watch SBER
watch
```

```
Добавлено в watchlist: SBER
Watchlist: [SBER]
```

## quote

Получить котировки по одному или нескольким тикерам (из `QuoteSource` — заглушки без реальной сети).

```
quote <TICKER> [TICKER...]
```

```bash
quote SBER GAZP
```

```
SBER: 250.00 RUB ▲ 1.20%
GAZP: 120.50 RUB ▼ 0.80%
```

Котировка есть только для `SBER` и `GAZP` (заготовлены в `Main.sampleQuotes()`); для остальных
тикеров — `Котировка не найдена: <TICKER>`.

## remove

Делистинг: точечно по тикеру либо массово по валюте. После удаления из watchlist автоматически
прибираются «висячие» тикеры удалённых инструментов (актуально только в рамках одного запуска
`Main`, где `repository` и `watchlist` общие — каждый вызов CLI из командной строки поднимает их
заново, поэтому между отдельными запусками watchlist пуст).

```
remove <TICKER>
remove --currency CURRENCY
```

```bash
remove SBER
```

```
Удалено: SBER
```

```bash
remove --currency RUB
```

```
Удалено (RUB): [SBER, OFZ26233]
```

Без тикера и без `--currency` — ошибка («Укажите тикер или --currency»).

## save

Сохранить текущий список инструментов в файл (одна строка на инструмент, `toString()`).

```
save <PATH>
```

```bash
save instruments.txt
```

```
Сохранено инструментов: 3 (instruments.txt)
```
