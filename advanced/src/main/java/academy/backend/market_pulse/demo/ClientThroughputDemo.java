package academy.backend.market_pulse.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.client.impl.HttpUrlConnectionTInvestApiClient;
import academy.backend.market_pulse.client.impl.reactor.ReactorTInvestApiClient;
import academy.backend.market_pulse.config.TInvestApiProperties;

/**
 * Живая демонстрация: три способа сходить за
 * котировками {@code N} раз к моковому серверу и измерить пропускную способность — без JMH, просто
 * по часам ({@link System#nanoTime()}), чтобы цифры менялись на глазах у группы:
 *
 * <ol>
 *   <li><b>Один поток, N запросов</b> — блокирующий {@link HttpUrlConnectionTInvestApiClient} зовут
 *       в цикле один за другим: каждый запрос ждёт предыдущий;
 *   <li><b>Многопоточность</b> — тот же блокирующий клиент, но запросы раздаются пулу потоков
 *       (поток на запрос);
 *   <li><b>Reactor</b> — все N запросов уходят подряд в один поток событийного цикла, а вызывающий
 *       поток ждёт один раз на весь пакет ({@code fetchQuotesBatch}).
 * </ol>
 *
 * <p>Запуск: {@code ClientThroughputDemo [запросов] [размер пула]} (по умолчанию 30 и 10). Моковый
 * сервер (Docker, см. {@code docker/mockserver}) должен быть поднят и отвечать с задержкой порядка
 * 300 мс — иначе разницу не видно. Перед каждым замером выполняется один прогревочный запрос
 * (рукопожатие, загрузка классов), чтобы не мерить разовые накладные.
 */
public class ClientThroughputDemo {

    private static final int DEFAULT_REQUESTS = 30;
    private static final int DEFAULT_POOL_SIZE = 10;

    public static void main(String[] args) throws Exception {
        int requests = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_REQUESTS;
        int poolSize = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_POOL_SIZE;
        TInvestApiProperties properties = TInvestApiProperties.localMock();

        System.out.printf("Запросов: %d. Размер пула для многопоточного варианта: %d.%n%n", requests, poolSize);

        HttpUrlConnectionTInvestApiClient blockingClient = new HttpUrlConnectionTInvestApiClient(properties);
        blockingClient.fetchQuotes();

        long oneThreadMs = measureOneThread(blockingClient, requests);
        report("1. Один поток, запросы подряд", requests, oneThreadMs);

        long poolMs = measureThreadPool(blockingClient, requests, poolSize);
        report("2. Многопоточность: пул из " + poolSize + " потоков", requests, poolMs);

        try (ReactorTInvestApiClient reactorClient = new ReactorTInvestApiClient(properties)) {
            reactorClient.fetchQuotes();
            long reactorMs = measureReactorBatch(reactorClient, requests);
            report("3. Reactor: один поток цикла", requests, reactorMs);
        }
    }

    private static long measureOneThread(TInvestApiClient client, int requests) {
        long start = System.nanoTime();
        for (int i = 0; i < requests; i++) {
            client.fetchQuotes();
        }
        return elapsedMillis(start);
    }

    private static long measureReactorBatch(ReactorTInvestApiClient client, int requests) {
        long start = System.nanoTime();
        client.fetchQuotesBatch(requests);
        return elapsedMillis(start);
    }

    private static long measureThreadPool(TInvestApiClient client, int requests, int poolSize)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try {
            List<Callable<Void>> tasks = new ArrayList<>(requests);
            for (int i = 0; i < requests; i++) {
                tasks.add(() -> {
                    client.fetchQuotes();
                    return null;
                });
            }
            long start = System.nanoTime();
            for (Future<Void> future : pool.invokeAll(tasks)) {
                try {
                    future.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new IllegalStateException(e.getCause());
                }
            }
            return elapsedMillis(start);
        } finally {
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.MINUTES);
        }
    }

    private static void report(String title, int requests, long millis) {
        double perSecond = requests * 1000.0 / Math.max(millis, 1);
        System.out.printf("%-42s %6d мс   %7.1f запросов/с%n", title, millis, perSecond);
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
