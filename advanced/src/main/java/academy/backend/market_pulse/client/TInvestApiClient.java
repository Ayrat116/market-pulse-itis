package academy.backend.market_pulse.client;

import java.util.List;

import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.model.Quote;

/**
 * Клиент к {@code instruments-service} T-Invest API (за моковым сервером в Docker). Один интерфейс,
 * три реализации (см. пакет {@code client.impl}): блокирующая на {@code HttpURLConnection},
 * реакторная на {@code Selector} ({@code ReactorTInvestApiClient}) и готовая на {@code HttpClient} —
 * вызывающий код не знает и не должен знать, какая из них используется под {@link TInvestClientMode}.
 */
public interface TInvestApiClient {

    /**
     * Текущий набор инструментов с котировками. Бросает {@link TInvestApiException} при сетевой
     * ошибке, неожиданном статусе ответа или битом JSON.
     */
    List<Quote> fetchQuotes();
}
