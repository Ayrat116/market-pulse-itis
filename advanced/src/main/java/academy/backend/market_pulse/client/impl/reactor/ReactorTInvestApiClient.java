package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import academy.backend.market_pulse.client.InstrumentQuoteMapper;
import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;
import academy.backend.market_pulse.exception.TInvestApiException;
import academy.backend.market_pulse.model.Quote;

/**
 * Реакторная реализация {@link TInvestApiClient}: каркас вокруг {@link EventLoop} (он сам владеет
 * {@link Demultiplexer}) и обработчиков ({@link ConnectHandler}, {@link WriteHandler},
 * {@link ReadHandler}). Вызывающий поток блокируется на ответе
 * ({@link Exchange#await()}), но пока он ждёт, один поток цикла обслуживает сразу все запросы.
 *
 * <p>Выгода видна на {@link #fetchQuotesBatch(int)}: запросы уходят подряд, не дожидаясь друг друга,
 * а ждём мы один раз. Асинхронный интерфейс (запрос «потом») убрал бы и это ожидание — к нему вернёмся
 * в теме многопоточности.
 */
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

    /**
     * {@code requests} запросов подряд: сначала все отправляются, потом ждём ответы. Один поток цикла
     * ведёт все соединения одновременно — время близко к задержке одного запроса, а не к сумме.
     */
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

    /**
     * Не блокирует: открывает канал, начинает подключение и отдаёт его под наблюдение циклу событий.
     * Дальше запрос ведут обработчики в потоке цикла.
     */
    private Exchange submit() {
        // NOTICE: канал неблокирующий — connect() вернётся сразу, рукопожатие пойдёт в фоне; OP_CONNECT
        // сообщит о его завершении. Первое состояние запроса — ConnectHandler.
        // NOTICE: сокеты не пулируем: на каждый запрос открывается новый канал (новое TCP-рукопожатие),
        // а после ответа он закрывается (Connection: close). Общие для всех запросов — только поток цикла и
        // селектор. Пул соединений (keep-alive) потребовал бы разбирать Content-Length/chunked, чтобы
        // знать, где кончается ответ, не дожидаясь закрытия соединения, а также таймауты простоя и
        // проверку живости — всё это есть в HttpClient, но не в нашей учебной реализации.
        byte[] request = RawHttpProtocol.buildGetRequest(
                address.getHostName(), address.getPort(), "/v1/instruments", mockScenario);
        try {
            SocketChannel channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(address);
            Exchange exchange = new Exchange(channel, request);
            eventLoop.register(channel, SelectionKey.OP_CONNECT, new ConnectHandler(exchange));
            return exchange;
        } catch (IOException e) {
            throw new TInvestApiException("Не удалось открыть соединение с instruments-service", e);
        }
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
        eventLoop.shutdown();
    }
}
