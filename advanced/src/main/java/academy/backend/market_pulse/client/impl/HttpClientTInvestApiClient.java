package academy.backend.market_pulse.client.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import academy.backend.market_pulse.client.InstrumentQuoteMapper;
import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.model.Quote;

/**
 * Реализация {@link TInvestApiClient} на {@link HttpClient} из JDK 11+ — показывается после
 * Reactor: всё, что мы только что писали руками, здесь уже готово (свой событийный цикл на
 * {@code Selector}, пул соединений). {@link #fetchQuotes()} ({@code send}) блокирует вызывающий
 * поток до ответа. {@link HttpClient} создаётся один раз в конструкторе и переиспользуется.
 *
 * <p>NOTICE: у {@link HttpClient} есть и асинхронный интерфейс ({@code sendAsync}) — запрос не
 * блокирует вызывающий поток, результат приходит «потом». К нему вернёмся в теме многопоточности.
 */
public class HttpClientTInvestApiClient implements TInvestApiClient {

    private final HttpClient httpClient;
    private final TInvestApiProperties properties;

    public HttpClientTInvestApiClient(TInvestApiProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    // NOTICE: HttpClient создаётся один раз и переиспользуется — внутри у него пул соединений и собственный
    //  цикл событий на Selector; создавать клиент на каждый запрос значит терять всё это.
    /** {@code send} блокирует вызывающий поток до ответа. */
    @Override
    public List<Quote> fetchQuotes() {
        HttpResponse<String> response;
        try {
            response = httpClient.send(buildRequest(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new TInvestApiException("Не удалось выполнить запрос к instruments-service", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TInvestApiException("Запрос к instruments-service прерван", e);
        }
        return toQuotes(response);
    }

    private HttpRequest buildRequest() {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/v1/instruments"))
                .timeout(properties.getRequestTimeout())
                .header("Accept", "application/json")
                .GET();
        if (properties.getMockScenario() != null) {
            requestBuilder.header(TInvestApiProperties.MOCK_SCENARIO_HEADER, properties.getMockScenario());
        }
        return requestBuilder.build();
    }

    @Override
    public CompletableFuture<List<Quote>> fetchQuotesAsync() {
        return CompletableFuture.completedFuture(List.of());
    }

    private List<Quote> toQuotes(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            throw new TInvestApiException(
                    "instruments-service вернул неожиданный статус: " + response.statusCode());
        }
        return InstrumentQuoteMapper.fromJson(response.body());
    }

}
