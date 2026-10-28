package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/** Второе состояние запроса: канал готов принимать данные, отправляем запрос ({@code OP_WRITE}). */
final class WriteHandler extends ExchangeHandler {

    WriteHandler(Exchange exchange) {
        super(exchange);
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        // NOTICE: write() может записать НЕ ВСЁ — сколько влезло в буфер сокета, столько и ушло.
        // Пока в requestBuffer остались данные, остаёмся в OP_WRITE: селектор разбудит нас снова, когда
        // сокет опять сможет принимать. Когда буфер опустел — запрос отправлен, переключаемся на
        // чтение (OP_READ) и вешаем на ключ ReadHandler.
        exchange.channel.write(exchange.requestBuffer);
        if (!exchange.requestBuffer.hasRemaining()) {
            key.interestOps(SelectionKey.OP_READ); // запрос отправлен целиком — ждём ответ
            key.attach(new ReadHandler(exchange));
        }
    }
}
