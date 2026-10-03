package academy.backend.market_pulse.cli;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;

import academy.backend.market_pulse.exception.InstrumentSaveException;
import academy.backend.market_pulse.model.Instrument;
import academy.backend.market_pulse.repository.InstrumentRepository;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Сохраняет текущий список инструментов в файл по указанному пользователем пути — первый в
 * проекте настоящий {@link AutoCloseable} ресурс (файл), а не игрушечная демонстрация.
 */
@Command(name = "save", description = "Сохранение инструментов из репозитория в файл")
@RequiredArgsConstructor
public class SaveCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Путь к файлу для сохранения")
    private String path;

    private final InstrumentRepository repository;

    @Override
    public Integer call() {
        try {
            // NOTICE: Try.withResources — тот же try-with-resources: writer закрывается автоматически,
            //  а любое исключение (в том числе checked IOException) оказывается внутри Try и
            //  оборачивается в InstrumentSaveException с сохранением причины.
            int saved = Try.withResources(() -> new BufferedWriter(new FileWriter(path)))
                    .of(this::writeAll)
                    .getOrElseThrow(cause -> new InstrumentSaveException(path, cause));
            System.out.println("Сохранено инструментов: " + saved + " (" + path + ")");
            return 0;
        } catch (InstrumentSaveException e) {
            System.out.println(e.getMessage());
            return 1;
        }
    }

    private int writeAll(BufferedWriter writer) throws IOException {
        // NOTICE: repository.forEach(instrument -> writer.write(...)) не скомпилируется:
        //  Consumer не объявляет checked IOException, поэтому здесь обычный цикл.
        int count = 0;
        for (Instrument instrument : repository) {
            writer.write(instrument.toString());
            writer.newLine();
            count++;
        }
        return count;
    }
}
