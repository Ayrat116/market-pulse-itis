package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/** Третье состояние запроса: читаем ответ по мере поступления ({@code OP_READ}). */
final class ReadHandler extends ExchangeHandler {

    private final ByteBuffer buffer = ByteBuffer.allocate(4096);

    ReadHandler(Exchange exchange) {
        super(exchange);
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        // NOTICE: ответ приходит кусками, поэтому читаем в цикле, пока в сокете есть данные
        // (read() > 0), и копим в exchange.responseBuffer; 0 — данные закончились, ждём следующего
        // OP_READ. Селектор у нас level-triggered: если не дочитали, он разбудит нас снова, поэтому
        // и один read() за событие тоже корректен — цикл просто экономит лишние select(). Цикл читает
        // ограниченный буфер за раз и не должен «застрять» на быстром отправителе, иначе остальные
        // соединения будут ждать.
        // -1 означает, что сервер закрыл соединение (Connection: close), то есть ответ получен целиком:
        // закрыть канал и вызвать exchange.complete(). Канал не возвращается в пул и не переиспользуется
        // — на следующий запрос будет новое соединение.
        // TODO: цикл read(buffer) -> flip -> копим в responseBuffer -> clear; на -1 — close + complete
        throw new UnsupportedOperationException("ReadHandler.handle ещё не реализован");
    }
}
