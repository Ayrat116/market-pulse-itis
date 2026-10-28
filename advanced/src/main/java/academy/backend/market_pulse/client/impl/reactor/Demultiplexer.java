package academy.backend.market_pulse.client.impl.reactor;

import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class Demultiplexer implements AutoCloseable {

    private final Selector selector;
    private final Queue<Registration> queue;

    @SneakyThrows
    Demultiplexer() {
        this.selector = Selector.open();
        this.queue = new ConcurrentLinkedQueue<>();
    }

    void register(SocketChannel channel, int ops, Handler handler) {
        queue.add(new Registration(channel, ops, handler));
        selector.wakeup();
    }

    @SneakyThrows
    List<SelectionKey> select() {
        registerNewChannels();
        return selectChannelEvents();
    }

    private void registerNewChannels() throws ClosedChannelException {
        Registration registration;
        while ((registration = queue.poll()) != null) {
            registration.channel().register(selector, registration.ops(), registration.handler());
        }
    }

    private List<SelectionKey> selectChannelEvents() throws IOException {
        selector.select();
        var keys = new ArrayList<>(selector.selectedKeys());
        selector.selectedKeys().clear();
        return keys;
    }

    @Override
    public void close() throws Exception {
        selector.close();
    }

    private record Registration(SocketChannel channel, int ops, Handler handler) {

    }

}
