package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Один поток, один {@link Selector}, сколько угодно одновременных запросов. Это и есть главная
 * идея: вместо потока на запрос — один поток следит сразу за всеми сокетами и реагирует только на
 * те, что готовы.
 *
 * <p>Новый запрос не ждёт своей очереди на текущий {@link Selector#select()} — {@link #submit}
 * кладёт канал в очередь регистрации и будит цикл через {@link Selector#wakeup()}, вместо того
 * чтобы регистрировать канал напрямую из вызывающего потока (регистрация и {@code select()} не
 * должны происходить из разных потоков одновременно без синхронизации).
 */
public final class Reactor implements AutoCloseable {

    private final Selector selector;
    private final Queue<PendingExchange> pendingRegistrations = new ConcurrentLinkedQueue<>();
    private final Thread loopThread;
    private volatile boolean running = true;

    public Reactor() {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось открыть Selector", e);
        }
        this.loopThread = new Thread(this::runLoop, "reactor-event-loop");
        this.loopThread.setDaemon(true);
    }

    public void start() {
        loopThread.start();
    }

    /**
     * Отправляет запрос и возвращает {@link CompletableFuture}, который завершится сырым телом
     * ответа — не блокирует вызывающий поток ни на подключение, ни на ожидание ответа.
     */
    public CompletableFuture<byte[]> submit(InetSocketAddress address, byte[] requestBytes) {
        // NOTICE: вызывающий поток не блокируется: канал открывается в неблокирующем режиме,
        // connect() возвращается сразу (рукопожатие идёт в фоне). Регистрировать канал в selector
        // прямо отсюда нельзя — цикл в этот момент может спать в select(), а register() и select()
        // из разных потоков без синхронизации не дружат. Поэтому: очередь регистрации +
        // selector.wakeup(), который прерывает текущий select(), и цикл сам подберёт новый канал.
        // TODO: SocketChannel.open + configureBlocking(false) + connect, PendingExchange в
        //  pendingRegistrations, selector.wakeup(), вернуть exchange.future
        throw new UnsupportedOperationException("Reactor.submit ещё не реализован");
    }

    private void runLoop() {
        // NOTICE: один поток на ВСЕ соединения — вся идея Reactor в этом цикле: забрать новые каналы
        // из очереди, заснуть в select() до первого готового события, раздать готовые ключи
        // обработчику и обязательно очистить selectedKeys() — Selector сам их не убирает.
        // close() закрывает Selector под работающим циклом — это штатное завершение
        // (ClosedSelectorException при running == false), не сбой.
        // TODO: while (running) { registerPending(); select(); для каждого selectedKey — dispatch(key),
        //  ошибки — failAndClose; selectedKeys().clear() }
        throw new UnsupportedOperationException("Reactor.runLoop ещё не реализован");
    }

    private void registerPending() {
        PendingExchange exchange;
        while ((exchange = pendingRegistrations.poll()) != null) {
            try {
                exchange.channel.register(selector, SelectionKey.OP_CONNECT, exchange);
            } catch (ClosedChannelException e) {
                exchange.future.completeExceptionally(e);
            }
        }
    }

    private void dispatch(SelectionKey key) throws IOException {
        // NOTICE: состояние запроса (PendingExchange) лежит в key.attachment(), а «что делаем
        // дальше» задаёт interestOps: OP_CONNECT -> finishConnect() -> OP_WRITE -> write() пока буфер
        // не опустеет (write может записать НЕ ВСЁ) -> OP_READ -> read() копим по кускам, а -1 означает,
        // что сервер закрыл соединение (Connection: close), то есть ответ получен целиком.
        // TODO: три ветки isConnectable / isWritable / isReadable и переключение interestOps
        throw new UnsupportedOperationException("Reactor.dispatch ещё не реализован");
    }

    private void failAndClose(SelectionKey key, Exception cause) {
        PendingExchange exchange = (PendingExchange) key.attachment();
        key.cancel();
        try {
            key.channel().close();
        } catch (IOException ignored) {
            // канал и так не рабочий — закрыть по возможности, вторичная ошибка здесь не интересна
        }
        if (exchange != null) {
            exchange.future.completeExceptionally(cause);
        }
    }

    @Override
    public void close() {
        running = false;
        selector.wakeup();
        try {
            selector.close();
        } catch (IOException ignored) {
            // завершаем работу в любом случае
        }
    }
}
