package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.util.List;

/**
 * Цикл событий — «сердце» реактора: один поток на все соединения. Ждёт у {@link Demultiplexer},
 * получает готовые ключи и раздаёт каждый своему {@link Handler}.
 *
 * <p>NOTICE: наследование от {@link Thread} — упрощение для наглядности (цикл «и есть» поток). В
 * промышленном коде цикл получает поток от исполнителя, а не наследуется от него.
 */
public final class EventLoop extends Thread {

    private final Demultiplexer demultiplexer;
    private volatile boolean running = true;

    public EventLoop(Demultiplexer demultiplexer) {
        super("reactor-event-loop");
        this.demultiplexer = demultiplexer;
        setDaemon(true);
    }

    @Override
    public void run() {
        // NOTICE: вся идея Reactor в этом цикле: пока работаем — спросить у демультиплексора готовые
        // ключи и каждый отдать обработчику, который лежит в key.attachment(). Ошибка в одном
        // обработчике не должна ронять цикл: её получает onError этого же обработчика
        // (IOException и CancelledKeyException). close() закрывает селектор под работающим циклом —
        // это штатное завершение (ClosedSelectorException при running == false), не сбой.
        // Внутри обработчика нельзя блокироваться и нельзя считать долго — иначе встанут все соединения.
        // TODO: while (running) { для каждого ключа из demultiplexer.select() — ((Handler) key.attachment()).handle(key) }
        throw new UnsupportedOperationException("EventLoop.run ещё не реализован");
    }

    /** Останавливает цикл: снимает флаг и закрывает демультиплексор, будя спящий {@code select()}. */
    public void shutdown() {
        running = false;
        demultiplexer.close();
    }
}
