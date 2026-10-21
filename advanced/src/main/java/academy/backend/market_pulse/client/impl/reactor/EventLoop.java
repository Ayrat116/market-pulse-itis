package academy.backend.market_pulse.client.impl.reactor;

import java.nio.channels.SelectableChannel;

public final class EventLoop extends Thread {

    public void register(SelectableChannel channel, int ops, Handler handler) {
        throw new UnsupportedOperationException("EventLoop.register ещё не реализован");
    }

    public void shutdown() {
        throw new UnsupportedOperationException("EventLoop.shutdown ещё не реализован");
    }
}
