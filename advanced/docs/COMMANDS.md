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

Котировка есть для `SBER`, `GAZP`, `LKOH`, `AAPL`, `OFZ26233`, `FXUS` (заготовлены в
`AppConfig.sampleQuotes()`); для остальных тикеров — `Котировка не найдена: <TICKER>`.

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

## stats

Печатает все зарегистрированные статистики — каждая строка: код статистики и её результат над шестью
котировками `AppConfig.sampleQuotes()`.

```
stats
```

```bash
stats
```

```
COUNT_BY_TYPE: {ETF=1, BOND=1, STOCK=4}
COUNT_BY_CURRENCY: {USD=2, RUB=4}
AVERAGE_CHANGE: 0.6583333333333333
TOTAL_PRICE: 8482.65
GAIN_LOSS_SPLIT: {false=2, true=4}
```

Новая статистика добавляется без изменения `StatsCommand`: класс в `stat.impl` + значение в `StatCode` + строка в
`Main.stats()`.

## compare

Сравнить доходность инструментов по тикерам — по убыванию `changePercent`; тикеры, которых нет в
`QuoteSource`, молча пропускаются.

```
compare <TICKER> [TICKER...]
```

```bash
compare SBER GAZP LKOH
```

```
LKOH: 7100.00 RUB ▲ 2.30%
SBER: 250.00 RUB ▲ 1.20%
GAZP: 120.50 RUB ▼ 0.80%
```

## movers

Топ инструментов по изменению цены среди всех котировок `QuoteSource` — по умолчанию растущие,
`--losers` — упавшие.

```
movers [--top N] [--losers]
```

```bash
movers --top 3
movers --top 2 --losers
```

```
LKOH: 7100.00 RUB ▲ 2.30%
SBER: 250.00 RUB ▲ 1.20%
FXUS: 6.85 USD ▲ 0.90%
```

## history

Скользящее среднее по истории цен инструмента — через Gatherer API (`MovingAverageGatherer`). История
цен — заглушка-`Map` в `AppConfig.samplePriceHistory()`, есть только для `SBER` и `LKOH`.

```
history <TICKER> [--sma K]
```

```bash
history SBER --sma 3
```

```
Цены: [244.00, 246.50, 245.00, 248.00, 250.00, 249.50, 251.20]
SMA(3): [245.17, 246.50, 247.67, 249.17, 250.23]
```

## update

Забирает актуальные котировки у `instruments-service` через `TInvestApiClient` и печатает их. Работает
против мокового сервера в Docker (`docker/mockserver`, порт 1080) — поднять заранее:

```bash
docker compose -f docker/mockserver/docker-compose.yml up -d
```

```
update
```

Реализацию клиента выбирает переменная окружения `MARKET_PULSE_CLIENT`: `URL_CONNECTION` (блокирующий
`HttpURLConnection`), `REACTOR` (самописный на `Selector`) или `HTTP_CLIENT` (по умолчанию, готовый `HttpClient`).
Сбой на стороне мока включает `MARKET_PULSE_MOCK_SCENARIO=error|timeout`.

```bash
MARKET_PULSE_CLIENT=URL_CONNECTION update
```

```
SBER: 250.00 RUB ▲ 1.20%
GAZP: 120.50 RUB ▼ 0.80%
Получено котировок: 2
```
