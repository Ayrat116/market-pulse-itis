package academy.backend.market_pulse.demo;

import java.math.BigDecimal;

import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Stock;
import academy.backend.market_pulse.proxy.ProxyFactory;
import academy.backend.market_pulse.repository.InMemoryInstrumentRepository;
import academy.backend.market_pulse.repository.InstrumentRepository;

/**
 * JDK Dynamic Proxy + {@code LinkedHashMap} в режиме LRU: кеширующая обёртка над
 * {@link InstrumentRepository}, созданная {@link ProxyFactory#cachingRepository}. Размер кеша
 * намеренно маленький (2), чтобы вытеснение произошло за несколько вызовов, а не за тысячи.
 * Не внедряется в проект — только демонстрация.
 */
public class CachingProxyDemo {

    public static void main(String[] args) {
        InstrumentRepository repository = new InMemoryInstrumentRepository();
        repository.add(new Stock("GAZP", "Газпром", Currency.RUB, "Energy", new BigDecimal("4.0")));
        repository.add(new Stock("LKOH", "Лукойл", Currency.RUB, "Energy", new BigDecimal("3.2")));
        repository.add(new Stock("YNDX", "Яндекс", Currency.RUB, "Technology", new BigDecimal("1.0")));

        InstrumentRepository cached = ProxyFactory.cachingRepository(repository, 2);

        System.out.println("1. Первый запрос GAZP — промах, идёт в target:");
        cached.findByTicker("GAZP");

        System.out.println();
        System.out.println("2. Повторный запрос GAZP — попадание, из кеша:");
        cached.findByTicker("GAZP");

        System.out.println();
        System.out.println("3. Запрос LKOH — промах, в кеше теперь {GAZP, LKOH}:");
        cached.findByTicker("LKOH");

        System.out.println();
        System.out.println("4. Запрос YNDX — кеш переполнен (размер 2), вытесняется самый давно");
        System.out.println("   не запрашиваемый — GAZP (LKOH был запрошен позже):");
        cached.findByTicker("YNDX");

        System.out.println();
        System.out.println("5. Повторный запрос GAZP — снова промах, его только что вытеснили:");
        cached.findByTicker("GAZP");
    }
}
