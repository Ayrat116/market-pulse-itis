# Моковый instruments-service (Docker)

Имитирует ручку `GET /v1/instruments` T-Invest API: настоящий API требует токен, а для локальной
разработки и замеров нужен предсказуемый сервер. Поведение переключается заголовком
`X-Mock-Scenario`. Сценарий по умолчанию отвечает с задержкой **300 мс** — специально не мгновенно,
иначе не будет видно разницы между блокирующим клиентом и неблокирующим.

## Запуск

```bash
docker compose -f docker/mockserver/docker-compose.yml up -d
```

Поднимется на `http://localhost:1080` — адрес, который подставлен по умолчанию в
`TInvestApiProperties.localMock()`.

## Проверка

```bash
curl http://localhost:1080/v1/instruments                                   # ~300 мс, 6 инструментов
curl -H "X-Mock-Scenario: error" http://localhost:1080/v1/instruments       # мгновенно, 500
curl -H "X-Mock-Scenario: timeout" http://localhost:1080/v1/instruments     # 10 секунд, затем []
```

## Сбой из приложения

Клиенты приложения сами отправляют `X-Mock-Scenario`, если задана переменная окружения
`MARKET_PULSE_MOCK_SCENARIO` (`error` | `timeout` | `ok`; пусто — заголовок не отправляется):

```bash
MARKET_PULSE_MOCK_SCENARIO=error java -cp <classpath> academy.backend.market_pulse.Main update
```

## Остановка

```bash
docker compose -f docker/mockserver/docker-compose.yml down
```

## Примечание

Образ `mockserver/mockserver:5.15.0` и формат `expectations.json` (ключ `delay`, вложенный
`body.json`) должны совпадать с версией MockServer, которую вы запускаете. Перед использованием
имеет смысл проверить все три `curl`-команды выше.
