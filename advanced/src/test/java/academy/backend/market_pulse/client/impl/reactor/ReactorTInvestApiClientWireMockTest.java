package academy.backend.market_pulse.client.impl.reactor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import academy.backend.market_pulse.config.TInvestApiProperties;
import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.model.Quote;

/**
 * {@link ReactorTInvestApiClient} против WireMock: настоящий HTTP по настоящему порту, заглушки —
 * из теста. Пока {@link Demultiplexer}, {@link EventLoop} и обработчики не написаны, тесты красные;
 * когда реализация готова — зелёные.
 */
class ReactorTInvestApiClientWireMockTest {

    private static final String INSTRUMENTS = "/v1/instruments";
    private static final String TWO_INSTRUMENTS = """
            [
              {"ticker": "SBER", "name": "Сбер", "type": "STOCK", "currency": "RUB", "price": 250.00, "changePercent": 1.20},
              {"ticker": "FXUS", "name": "FinEx FXUS", "type": "ETF", "currency": "USD", "price": 6.85, "changePercent": 0.90}
            ]
            """;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ReactorTInvestApiClient client;

    @BeforeEach
    void setUp() {
        client = new ReactorTInvestApiClient(properties());
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    private static TInvestApiProperties properties() {
        return new TInvestApiProperties("localhost", wireMock.getPort(), Duration.ofSeconds(2), Duration.ofSeconds(2));
    }

    @Test
    void parsesInstrumentsFromTheResponse() {
        wireMock.stubFor(get(urlEqualTo(INSTRUMENTS))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TWO_INSTRUMENTS)));

        List<Quote> quotes = client.fetchQuotes();

        assertThat(quotes)
                .extracting(q -> q.getInstrument().getTicker(), q -> q.getInstrument().getType())
                .containsExactly(tuple("SBER", InstrumentType.STOCK), tuple("FXUS", InstrumentType.ETF));
    }

    @Test
    void asksForJsonAndSendsExactlyOneRequest() {
        wireMock.stubFor(get(urlEqualTo(INSTRUMENTS)).willReturn(aResponse().withStatus(200).withBody("[]")));

        client.fetchQuotes();

        wireMock.verify(1, getRequestedFor(urlEqualTo(INSTRUMENTS))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void forwardsTheMockScenarioHeaderWhenConfigured() {
        wireMock.stubFor(get(urlEqualTo(INSTRUMENTS)).willReturn(aResponse().withStatus(200).withBody("[]")));

        try (ReactorTInvestApiClient scenarioClient = new ReactorTInvestApiClient(properties().withMockScenario("error"))) {
            scenarioClient.fetchQuotes();
        }

        wireMock.verify(getRequestedFor(urlEqualTo(INSTRUMENTS))
                .withHeader(TInvestApiProperties.MOCK_SCENARIO_HEADER, equalTo("error")));
    }

    @Test
    void emptyCatalogIsAnEmptyList() {
        wireMock.stubFor(get(urlEqualTo(INSTRUMENTS)).willReturn(aResponse().withStatus(200).withBody("[]")));

        assertThat(client.fetchQuotes()).isEmpty();
    }

    @ParameterizedTest(name = "статус {0} -> TInvestApiException")
    @ValueSource(ints = {400, 404, 500, 503})
    void unexpectedStatusIsReportedAsAnApiException(int status) {
        wireMock.stubFor(get(urlEqualTo(INSTRUMENTS)).willReturn(aResponse().withStatus(status).withBody("{}")));

        assertThatThrownBy(() -> client.fetchQuotes())
                .isInstanceOf(TInvestApiException.class)
                .hasMessageContaining(String.valueOf(status));
    }

    @ParameterizedTest(name = "тело [{0}] -> TInvestApiException")
    @ValueSource(strings = {"", "not a json", "[{\"ticker\": }]", "{\"ticker\": \"SBER\"}"})
    void malformedBodyIsReportedAsAnApiException(String body) {
        wireMock.stubFor(get(urlEqualTo(INSTRUMENTS)).willReturn(aResponse().withStatus(200).withBody(body)));

        assertThatThrownBy(() -> client.fetchQuotes()).isInstanceOf(TInvestApiException.class);
    }

    @Test
    void brokenConnectionIsReportedAsAnApiException() {
        wireMock.stubFor(get(urlEqualTo(INSTRUMENTS))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> client.fetchQuotes()).isInstanceOf(TInvestApiException.class);
    }

    @Test
    void connectionRefusedIsReportedAsAnApiException() {
        TInvestApiProperties closedPort = new TInvestApiProperties("localhost", 1, Duration.ofSeconds(2), Duration.ofSeconds(2));

        try (ReactorTInvestApiClient refused = new ReactorTInvestApiClient(closedPort)) {
            assertThatThrownBy(refused::fetchQuotes).isInstanceOf(TInvestApiException.class);
        }
    }

    @Test
    void batchOfRequestsIsServedByOneEventLoopWithoutWaitingForEachOther() {
        int requests = 10;
        long delayMillis = 300;
        wireMock.stubFor(get(urlEqualTo(INSTRUMENTS))
                .willReturn(aResponse().withStatus(200).withBody(TWO_INSTRUMENTS).withFixedDelay((int) delayMillis)));

        long start = System.nanoTime();
        List<List<Quote>> results = client.fetchQuotesBatch(requests);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(results).hasSize(requests).allSatisfy(quotes -> assertThat(quotes).hasSize(2));
        wireMock.verify(requests, getRequestedFor(urlEqualTo(INSTRUMENTS)));
        // последовательно было бы >= 10 * 300 мс = 3 с; один цикл событий укладывается примерно в одну задержку
        assertThat(elapsedMillis).isLessThan(requests * delayMillis / 2);
    }
}
