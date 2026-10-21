package academy.backend.market_pulse.client.impl.reactor;

import academy.backend.market_pulse.client.InstrumentQuoteMapper;
import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.model.Quote;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ReactorTInvestApiClient implements TInvestApiClient, AutoCloseable {

    private final EventLoop eventLoop = new EventLoop();
    private final InetSocketAddress address;
    private final String mockScenario;

    public ReactorTInvestApiClient(TInvestApiProperties properties) {
        this.address = new InetSocketAddress(properties.getHost(), properties.getPort());
        this.mockScenario = properties.getMockScenario();
        this.eventLoop.start();
    }

    @Override
    public List<Quote> fetchQuotes() {
        return parseQuotes(submit().await());
    }

    public List<List<Quote>> fetchQuotesBatch(int requests) {
        List<Exchange> exchanges = new ArrayList<>(requests);
        for (int i = 0; i < requests; i++) {
            exchanges.add(submit());
        }
        List<List<Quote>> result = new ArrayList<>(requests);
        for (Exchange exchange : exchanges) {
            result.add(parseQuotes(exchange.await()));
        }
        return result;
    }

    private Exchange submit() {
        byte[] request = RawHttpProtocol.buildGetRequest(
                address.getHostName(), address.getPort(),
                "/v1/instruments", mockScenario);
        throw new UnsupportedOperationException();
    }

    private List<Quote> parseQuotes(byte[] rawResponse) {
        RawHttpProtocol.ParsedResponse response = RawHttpProtocol.parse(rawResponse);
        if (response.statusCode() != 200) {
            throw new TInvestApiException("instruments-service вернул неожиданный статус: " + response.statusCode());
        }
        return InstrumentQuoteMapper.fromJson(response.body());
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }
}
