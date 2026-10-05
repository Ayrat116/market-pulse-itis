package academy.backend.market_pulse.repository;

import academy.backend.market_pulse.model.Instrument;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface InstrumentRepository extends Iterable<Instrument> {

    void add(Instrument instrument);

    Optional<Instrument> findByTicker(String ticker);

    /**
     * Удаляет инструмент по тикеру (делистинг). {@code true}, если инструмент был и его удалили;
     * {@code false}, если такого тикера не было. Точечное удаление по известному ключу — без
     * обхода коллекции, {@code Iterator} тут не нужен.
     */
    boolean remove(String ticker);

    /**
     * Массовый делистинг: удаляет все инструменты, подходящие под {@code filter} (например, все в
     * заданной валюте), и возвращает тикеры удалённых. В отличие от {@link #remove(String)} —
     * реализация обязана пройтись по хранилищу и решить по каждому элементу, удалять его или нет,
     * а значит без {@code Iterator.remove()} (или {@code removeIf} поверх него) не обойтись.
     */
    Set<String> removeIf(Predicate<Instrument> filter);
}
