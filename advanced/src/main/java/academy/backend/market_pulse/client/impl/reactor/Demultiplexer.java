package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Демультиплексор событий: обёртка над {@link Selector}. Следит сразу за многими каналами и по
 * вызову {@link #select()} отдаёт те, что готовы. Всё, что знает об операционной системе
 * ({@code epoll}/{@code kqueue}/{@code select}), спрятано здесь.
 */
public final class Demultiplexer implements AutoCloseable {

    private record Registration(SelectableChannel channel, int ops, Handler handler) {
    }

    private final Selector selector;
    private final Queue<Registration> pendingRegistrations = new ConcurrentLinkedQueue<>();

    public Demultiplexer() {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось открыть Selector", e);
        }
    }

    /**
     * Ставит канал под наблюдение: {@code ops} — какие события интересны, {@code handler} — кто на
     * них отреагирует. Можно вызывать из любого потока.
     */
    public void register(SelectableChannel channel, int ops, Handler handler) {
        // NOTICE: регистрировать канал прямо отсюда нельзя — цикл в этот момент может спать в select(),
        // а register() и select() из разных потоков без синхронизации не дружат. Поэтому:
        // кладём запрос в очередь pendingRegistrations и будим цикл через selector.wakeup() —
        // он прервёт текущий select(), и цикл сам подберёт новый канал (см. registerPending()).
        // TODO: положить Registration в pendingRegistrations, selector.wakeup()
        throw new UnsupportedOperationException("Demultiplexer.register ещё не реализован");
    }

    /**
     * Ждёт готовности хотя бы одного канала и возвращает готовые ключи; вызывается только из потока
     * {@link EventLoop}. Блокируется до первого события или до {@link #register}.
     */
    public List<SelectionKey> select() throws IOException {
        // NOTICE: порядок такой — сначала забрать новые каналы из очереди (registerPending()), потом
        // заснуть в selector.select() до первого события. Готовые ключи нужно скопировать в список
        // и ОБЯЗАТЕЛЬНО очистить selectedKeys(): Selector сам их оттуда не убирает.
        // TODO: registerPending(); selector.select(); скопировать selectedKeys() в список; очистить selectedKeys()
        throw new UnsupportedOperationException("Demultiplexer.select ещё не реализован");
    }

    private void registerPending() {
        Registration registration;
        while ((registration = pendingRegistrations.poll()) != null) {
            try {
                registration.channel().register(selector, registration.ops(), registration.handler());
            } catch (ClosedChannelException e) {
                registration.handler().onError(e);
            }
        }
    }

    /** Закрывает селектор; цикл, спящий в {@link #select()}, получит {@code ClosedSelectorException}. */
    @Override
    public void close() {
        try {
            selector.close();
        } catch (IOException ignored) {
            // завершаем работу в любом случае
        }
    }
}
