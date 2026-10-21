package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;

/**
 * Общая часть трёх обработчиков одного запроса ({@link ConnectHandler}, {@link WriteHandler},
 * {@link ReadHandler}): они делят {@link Exchange} и одинаково реагируют на сбой.
 */
abstract class ExchangeHandler implements Handler {

    protected final Exchange exchange;

    protected ExchangeHandler(Exchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void onError(Exception cause) {
        try {
            exchange.channel.close(); // закрытие канала само снимает его ключи с селектора
        } catch (IOException ignored) {
            // канал и так не рабочий — вторичная ошибка здесь не интересна
        }
        exchange.fail(cause);
    }
}
