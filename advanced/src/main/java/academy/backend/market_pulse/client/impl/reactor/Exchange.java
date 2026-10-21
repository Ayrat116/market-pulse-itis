package academy.backend.market_pulse.client.impl.reactor;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

import academy.backend.market_pulse.exception.TInvestApiException;

/**
 * Состояние одного запроса, общее для трёх обработчиков: канал, недописанный буфер запроса, накопленный
 * ответ и «защёлка», на которой ждёт вызывающий поток. Один объект на один запрос.
 */
final class Exchange {

    final SocketChannel channel;
    final ByteBuffer requestBuffer;
    final ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();

    private final CountDownLatch done = new CountDownLatch(1);
    private volatile byte[] response;
    private volatile Exception failure;

    Exchange(SocketChannel channel, byte[] requestBytes) {
        this.channel = channel;
        this.requestBuffer = ByteBuffer.wrap(requestBytes);
    }

    /** Ответ получен целиком: разбудить вызывающий поток. */
    void complete() {
        response = responseBuffer.toByteArray();
        done.countDown();
    }

    /** Запрос не удался: разбудить вызывающий поток с ошибкой. */
    void fail(Exception cause) {
        failure = cause;
        done.countDown();
    }

    /**
     * Блокирует вызывающий поток до ответа или ошибки.
     *
     * <p>NOTICE: это единственное место, где вызывающий поток ждёт, — поток цикла событий при этом
     * свободен для остальных запросов. Блокировку здесь заменил бы асинхронный интерфейс
     * (обратный вызов или результат «потом»); к нему вернёмся в теме многопоточности.
     */
    byte[] await() {
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TInvestApiException("Ожидание ответа instruments-service прервано", e);
        }
        if (failure != null) {
            throw new TInvestApiException("Не удалось выполнить реакторный запрос к instruments-service", failure);
        }
        return response;
    }
}
