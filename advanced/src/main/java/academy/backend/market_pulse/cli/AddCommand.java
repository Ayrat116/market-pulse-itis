package academy.backend.market_pulse.cli;

import java.util.concurrent.Callable;

import academy.backend.market_pulse.dictionary.InstrumentType;
import academy.backend.market_pulse.exception.DuplicateTickerException;
import academy.backend.market_pulse.exception.InvalidInstrumentException;
import academy.backend.market_pulse.exception.UnsupportedInstrumentException;
import academy.backend.market_pulse.factory.InstrumentFactories;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "add", description = "Добавление инструмента в репозиторий")
@RequiredArgsConstructor
public class AddCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Тип инструмента (STOCK, BOND, ETF)")
    private InstrumentType type;

    @Parameters(index = "1", description = "Тикер инструмента")
    private String ticker;

    @Parameters(index = "2", description = "Название инструмента")
    private String name;

    @Parameters(index = "3", description = "Валюта инструмента (RUB, USD, EUR)")
    private Currency currency;

    private final InstrumentRepository repository;

    @Override
    public Integer call() {
        try {
            var instrument = InstrumentFactories.create(type, ticker, name, currency);
            repository.add(instrument);
            System.out.println("Добавлено: " + instrument.getDescription());
            return 0;
        } catch (DuplicateTickerException | InvalidInstrumentException | UnsupportedInstrumentException e) {
            // NOTICE: реакция на все три ошибки одинаковая (сообщить пользователю и вернуть код
            //  ошибки), поэтому один multi-catch, а не три одинаковых блока.
            System.out.println("Не удалось добавить инструмент: " + e.getMessage());
            return 1;
        }
    }
}
