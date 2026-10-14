package academy.backend.market_pulse.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * @param <K> тип ключа
 * @param <V> тип значения
 */
public class Registry<K, V> {

    private final Map<K, V> items = new HashMap<>();

    /**
     * Находит все реализации типа {@code V} через {@link ServiceLoader} и регистрирует их под
     * ключом, который вычисляет {@code keyExtractor} — вызывающему коду не нужно ни явно передавать
     * {@code V.class}, ни писать цикл с ручным {@code register(...)} самому.
     *
     * <p>Тип {@code V} нигде не передаётся явно — достать {@code Class<V>} можно только там, где
     * {@code V} ещё не стёрт: в generic-сигнатуре самого класса, а не метода или конструктора. Приём
     * известен как <em>super type token</em>: {@code Registry} нужно создавать не напрямую, а через
     * анонимный подкласс — {@code new Registry<String, InstrumentFactory>(keyExtractor) {}} — тогда у
     * ЭТОГО конкретного подкласса {@code getClass().getGenericSuperclass()} возвращает
     * {@code ParameterizedType} с реальными аргументами {@code String}/{@code InstrumentFactory}: они
     * часть сигнатуры сгенерированного класса, а не стираемого generic-вызова.
     *
     * @throws IllegalStateException если {@code Registry} создан напрямую, без анонимного
     *                               подкласса, — тип {@code V} в этом случае восстановить нечем
     */
    @SuppressWarnings("unchecked")
    public Registry(Function<V, K> keyExtractor) {
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType parameterized)) {
            throw new IllegalStateException(
                    "Registry нужно создавать через анонимный подкласс: "
                            + "new Registry<K, V>(keyExtractor) {} вместо new Registry<>(keyExtractor)");
        }
        Class<V> valueType = (Class<V>) parameterized.getActualTypeArguments()[1];
        for (V value : ServiceLoader.load(valueType)) {
            register(keyExtractor.apply(value), value);
        }
    }

    public void register(K key, V value) {
        items.put(key, value);
    }

    /**
     * {@code Optional} вместо {@code null}: отсутствие значения для ключа заявлено в сигнатуре,
     * а не обнаруживается по факту падения на {@code NullPointerException} у вызывающего кода.
     */
    public Optional<V> get(K key) {
        return Optional.ofNullable(items.get(key));
    }
}
