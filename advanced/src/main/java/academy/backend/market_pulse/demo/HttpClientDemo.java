package academy.backend.market_pulse.demo;

import java.util.List;

import academy.backend.market_pulse.client.impl.HttpClientTInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;
import academy.backend.market_pulse.model.Quote;

/**
 * Готовый {@code HttpClient} — то, что мы только что писали руками на {@code Selector}, уже внутри.
 * Запрос выполняется через {@code send}: поток вызывающего кода ждёт ответа. Моковый сервер
 * (Docker) поднять заранее.
 */
public class HttpClientDemo {

    public static void main(String[] args) {
        HttpClientTInvestApiClient client = new HttpClientTInvestApiClient(TInvestApiProperties.localMock());

        List<Quote> quotes = client.fetchQuotes();
        System.out.println("send: получено котировок — " + quotes.size());
    }
}
