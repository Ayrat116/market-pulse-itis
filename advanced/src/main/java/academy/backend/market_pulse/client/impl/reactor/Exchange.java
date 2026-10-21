package academy.backend.market_pulse.client.impl.reactor;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;

import academy.backend.market_pulse.exception.TInvestApiException;

final class Exchange {

    private record Outcome(byte[] response, Exception failure) {
    }

    final SocketChannel channel;
    final ByteBuffer requestBuffer;
    final ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();

    private final AtomicReference<Outcome> outcome = new AtomicReference<>();

    Exchange(SocketChannel channel, byte[] requestBytes) {
        this.channel = channel;
        this.requestBuffer = ByteBuffer.wrap(requestBytes);
    }

    void complete() {
        outcome.compareAndSet(null, new Outcome(responseBuffer.toByteArray(), null));
    }

    void fail(Exception cause) {
        outcome.compareAndSet(null, new Outcome(null, cause));
    }

    byte[] await() {
        Outcome result;
        while ((result = outcome.get()) == null) {
            Thread.onSpinWait();
        }
        if (result.failure() != null) {
            throw new TInvestApiException("Не удалось выполнить реакторный запрос к instruments-service",
                    result.failure());
        }
        return result.response();
    }
}
