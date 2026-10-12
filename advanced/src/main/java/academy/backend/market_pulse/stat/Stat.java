package academy.backend.market_pulse.stat;

import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.model.aware.StatCodeAware;

import java.util.List;

/**
 * Одна статистика по списку котировок — для команды {@code stats} (семинар 6).
 *
 * <p>Та же идея расширяемости, что у {@code InstrumentFilter}: новый вид отбора добавлялся через
 * новый класс, реализующий {@code InstrumentFilter}, без изменения существующих. Здесь — то же
 * самое для статистик: добавить новую статистику — значит написать новый класс, реализующий
 * {@link Stat}, и зарегистрировать его в списке {@code Main.stats()}; {@code StatsCommand} при
 * этом не меняется — он просто обходит список и печатает {@code getCode(): calculateStats(...)}
 * для каждого элемента.
 *
 * <p>Параметры конкретной статистики (например, порог или сторона сравнения) передаются через
 * конструктор реализации — ровно как {@code PriceFilter} получает {@code operator}/{@code threshold}
 * через конструктор, а {@code matches(Instrument)} остаётся методом с одним фиксированным
 * аргументом. {@link #calculateStats(List)} — тот же принцип: один фиксированный аргумент, вся
 * вариативность — в том, что по-разному реализует {@code calculateStats} и что возвращает тип
 * {@code S}, общий для конкретной статистики, но разный между статистиками (число, доля, {@code Map}
 * — какой тип нужен именно этой статистике).
 *
 * @param <S> тип результата конкретной статистики
 */
public interface Stat<S> extends StatCodeAware {

    S calculateStats(List<Quote> quotes);
}
