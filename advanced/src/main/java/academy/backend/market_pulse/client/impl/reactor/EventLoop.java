package academy.backend.market_pulse.client.impl.reactor;

/**
 * Цикл событий — «сердце» реактора: один поток на все соединения. Сам создаёт свой
 * {@link Demultiplexer}, ждёт у него готовые ключи и раздаёт каждый своему {@link Handler}.
 */
public final class EventLoop extends Thread {
}
