package academy.backend.market_pulse.client.impl.reactor;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;

import academy.backend.market_pulse.exception.TInvestApiException;

/**
 * Состояние одного запроса, общее для трёх обработчиков: канал, недописанный буфер запроса, накопленный
 * ответ и результат, который ждёт вызывающий поток. Один объект на один запрос.
 */
final class Exchange {

    /** Итог запроса: либо ответ, либо ошибка. */
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

    /** Ответ получен целиком: публикуем результат (первый итог выигрывает). */
    void complete() {
        outcome.compareAndSet(null, new Outcome(responseBuffer.toByteArray(), null));
    }

    /** Запрос не удался: публикуем ошибку (первый итог выигрывает). */
    void fail(Exception cause) {
        outcome.compareAndSet(null, new Outcome(null, cause));
    }

    /**
     * Блокирует вызывающий поток до ответа или ошибки: крутится в бесконечном цикле и проверяет, не
     * появился ли результат (busy waiting).
     *
     * <p>NOTICE: это единственное место, где вызывающий поток ждёт, — поток цикла событий при этом
     * свободен для остальных запросов. Активное ожидание жжёт процессор впустую — для учебного примера
     * оно нагляднее всего, а в реальном коде ждут через {@code wait/notify}, блокирующие очереди или
     * асинхронный интерфейс (обратный вызов, результат «потом»); к ним вернёмся в теме многопоточности.
     */
    byte[] await() {
        Outcome result;
        while ((result = outcome.get()) == null) {
            Thread.onSpinWait(); // подсказка процессору: мы крутимся в ожидании
        }
        if (result.failure() != null) {
            throw new TInvestApiException("Не удалось выполнить реакторный запрос к instruments-service",
                    result.failure());
        }
        return result.response();
    }
}
