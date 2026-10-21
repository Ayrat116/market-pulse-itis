package academy.backend.market_pulse.client.impl.reactor;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

final class EventLoop extends Thread implements AutoCloseable {

    private final Demultiplexer demultiplexer = new Demultiplexer();

    EventLoop() {
        super("reactor-event-loop");
        setDaemon(true);
    }

    @SneakyThrows
    Exchange submit(InetSocketAddress address, byte[] request) {
        var channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(address);
        var exchange = new Exchange(channel, request);
        demultiplexer.register(channel, SelectionKey.OP_CONNECT, new ConnectHandler(exchange));
        return exchange;
    }

    @Override
    public void run() {
        while (!isInterrupted() && isAlive()) {
            try {
                var keys = demultiplexer.select();
                for (var key : keys) {
                    var handler = (Handler) key.attachment();
                    try {
                        handler.handle(key);
                    } catch (Exception e) {
                        handler.onError(e);
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    @SneakyThrows
    public void close() {
        interrupt();
        demultiplexer.close();
    }
}
