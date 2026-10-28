package academy.backend.market_pulse.cli;

import lombok.RequiredArgsConstructor;
import java.util.Set;
import java.util.concurrent.Callable;

import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Watchlist;
import academy.backend.market_pulse.repository.InstrumentRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Делистинг инструментов: точечно по тикеру ({@link InstrumentRepository#remove}) либо массово
 * по валюте ({@link InstrumentRepository#removeIf}, единственный способ пройтись по неизвестному
 * заранее числу инструментов и убрать подходящие). В обоих случаях следом прибирается watchlist,
 * если в нём остались тикеры удалённых инструментов.
 */
@Command(name = "remove", description = "Удалить инструмент(ы) из репозитория (делистинг)")
@RequiredArgsConstructor
public class RemoveCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "Тикер инструмента")
    private String ticker;

    @Option(names = {"-c", "--currency"}, description = "Удалить все инструменты в указанной валюте")
    private Currency currency;

    private final InstrumentRepository repository;
    private final Watchlist watchlist;

    @Override
    public Integer call() {
        if (ticker == null && currency == null) {
            System.out.println("Укажите тикер или --currency");
            return 1;
        }

        if (currency != null) {
            Set<String> removed = repository.removeIf(instrument -> instrument.getCurrency() == currency);
            if (removed.isEmpty()) {
                System.out.println("Инструментов в валюте " + currency + " не найдено");
                return 1;
            }
            System.out.println("Удалено (" + currency + "): " + removed);
        } else {
            if (!repository.remove(ticker)) {
                System.out.println("Инструмент не найден: " + ticker.toUpperCase());
                return 1;
            }
            System.out.println("Удалено: " + ticker.toUpperCase());
        }

        Set<String> pruned = watchlist.prune(repository);
        if (!pruned.isEmpty()) {
            System.out.println("Убрано из watchlist как устаревшее: " + pruned);
        }
        return 0;
    }
}
