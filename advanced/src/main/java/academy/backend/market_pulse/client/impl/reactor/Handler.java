package academy.backend.market_pulse.client.impl.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface Handler {
    void handle(SelectionKey key) throws IOException;

    void onError(Exception cause);
}
