package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Обработчик события готовности: {@link EventLoop} достаёт его из {@link SelectionKey#attachment()}
 * и вызывает, когда {@link Demultiplexer} сообщил, что канал готов. Обработчик реагирует только на
 * «своё» событие и не блокируется — иначе встанут все соединения разом.
 */
public interface Handler {

    /** Реакция на готовое событие; ошибки ввода-вывода уходят в {@link #onError}. */
    void handle(SelectionKey key) throws IOException;

    /** Сбой при обработке: закрыть канал и сообщить об ошибке вызывающему потоку. */
    void onError(Exception cause);
}
