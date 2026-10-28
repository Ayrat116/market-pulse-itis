package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/** Первое состояние запроса: ждём, пока TCP-соединение установится ({@code OP_CONNECT}). */
final class ConnectHandler extends ExchangeHandler {

    ConnectHandler(Exchange exchange) {
        super(exchange);
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        // NOTICE: connect() в неблокирующем режиме вернулся сразу — рукопожатие шло в фоне. Селектор
        // сообщил OP_CONNECT, значит его можно завершить: finishConnect(). Дальше «что ждём»
        // меняется: нас больше не интересует connect, нужна возможность писать (OP_WRITE), а на
        // ключ вместо этого обработчика вешаем следующий — WriteHandler.
        exchange.channel.finishConnect();
        key.interestOps(SelectionKey.OP_WRITE); // соединились — теперь ждём возможности писать запрос
        key.attach(new WriteHandler(exchange));
    }
}
