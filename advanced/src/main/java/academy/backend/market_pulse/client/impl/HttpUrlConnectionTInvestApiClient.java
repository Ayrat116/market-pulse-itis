package academy.backend.market_pulse.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import academy.backend.market_pulse.client.InstrumentQuoteMapper;
import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.model.Quote;

/**
 * Опорная реализация {@link TInvestApiClient} на «старом» {@link HttpURLConnection}. Классическая
 * блокирующая модель: одно соединение — один поток, который стоит на
 * {@code getResponseCode()}/{@code read()} всё время ожидания ответа. Именно на ней видно, что
 * делать, когда запросов много.
 */
public class HttpUrlConnectionTInvestApiClient implements TInvestApiClient {

    private final TInvestApiProperties properties;

    public HttpUrlConnectionTInvestApiClient(TInvestApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<Quote> fetchQuotes() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(properties.getBaseUrl() + "/v1/instruments")
                    .toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
            connection.setReadTimeout((int) properties.getRequestTimeout().toMillis());
            connection.setRequestProperty("Accept", "application/json");
            if (properties.getMockScenario() != null) {
                connection.setRequestProperty(TInvestApiProperties.MOCK_SCENARIO_HEADER, properties.getMockScenario());
            }

            // NOTICE: getResponseCode() блокирует поток до прихода ответа — процессор свободен, а поток
            //  при этом занят одним-единственным соединением. Таймауты (connect/read) здесь обязательны:
            //  по умолчанию они бесконечные.
            int status = connection.getResponseCode();
            if (status != 200) {
                throw new TInvestApiException("instruments-service вернул неожиданный статус: " + status);
            }
            try (InputStream in = connection.getInputStream()) {
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return InstrumentQuoteMapper.fromJson(body);
            }
        } catch (IOException e) {
            throw new TInvestApiException("Не удалось выполнить запрос к instruments-service", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public CompletableFuture<List<Quote>> fetchQuotesAsync(){
        return CompletableFuture.completedFuture(List.of());
    }

}
