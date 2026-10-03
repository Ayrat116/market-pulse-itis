package academy.backend.market_pulse.proxy;

import academy.backend.market_pulse.repository.InMemoryInstrumentRepository;
import academy.backend.market_pulse.repository.InstrumentRepository;
import lombok.experimental.UtilityClass;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Скрывает от клиента, как именно устроена прокси-обёртка над {@link InstrumentRepository},
 * которая замеряет время каждого вызова и логирует его. Клиент указывает только {@link Kind} —
 * какой из трёх механизмов использовать; про {@code InvocationHandler}, {@code Enhancer} и
 * ручное оборачивание каждого метода знает фабрика, а не он.
 */
@UtilityClass
public final class ProxyFactory {

    public enum Kind {
        STATIC, JDK_DYNAMIC, CGLIB
    }

    public static InstrumentRepository timingRepository(Kind kind) {
        return switch (kind) {
            case STATIC -> staticProxy();
            case JDK_DYNAMIC -> jdkDynamicProxy(new InMemoryInstrumentRepository());
            case CGLIB -> cglibProxy();
        };
    }

    /**
     * Статический прокси: класс {@link TimingInstrumentRepository} вручную реализует
     * {@link InstrumentRepository} и оборачивает вызовы к целевому объекту.
     *
     * <p>Ограничения: нужен отдельный класс-обёртка на каждый оборачиваемый интерфейс.
     * <p>Плюсы: полностью прозрачный код, никакой рефлексии, ошибки ловятся компилятором.
     * <p>Минусы: много шаблонного кода; при изменении интерфейса обёртку нужно править вручную.
     */
    private static InstrumentRepository staticProxy() {
        throw new UnsupportedOperationException();
    }

    /**
     * JDK-прокси на основе {@link Proxy} и обработчика вызовов ({@code InvocationHandler}).
     *
     * <p>Ограничения: проксировать можно только интерфейсы — у целевого объекта должен быть
     * хотя бы один интерфейс, проксировать конкретный класс без интерфейса нельзя.
     * <p>Плюсы: входит в стандартную библиотеку (без сторонних зависимостей), один обработчик
     * закрывает сразу все методы интерфейса.
     * <p>Минусы: каждый вызов идёт через рефлексию ({@code Method.invoke}) — медленнее прямого.
     */
    private static InstrumentRepository jdkDynamicProxy(InstrumentRepository target) {
        return (InstrumentRepository) Proxy.newProxyInstance(
                InstrumentRepository.class.getClassLoader(),
                new Class<?>[]{InstrumentRepository.class},
                (Object proxy, Method method, Object[] args) -> {
                    long start = System.nanoTime();
                    Object result = method.invoke(target, args);
                    System.out.printf("%s() выполнен за %d нс%n", method.getName(), System.nanoTime() - start);
                    return result;
                });
    }

    /**
     * CGLIB-прокси: {@link Enhancer} генерирует на лету подкласс
     * {@link InMemoryInstrumentRepository} и перехватывает вызовы его методов.
     *
     * <p>Ограничения: целевой класс и переопределяемые методы не должны быть {@code final};
     * у класса должен быть доступный конструктор без аргументов.
     * <p>Плюсы: можно проксировать конкретный класс без интерфейса; после генерации подкласса
     * вызовы быстрее, чем через reflection JDK-прокси.
     * <p>Минусы: дополнительная зависимость (cglib), сгенерированный байткод сложнее отлаживать.
     */
    private static InstrumentRepository cglibProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(InMemoryInstrumentRepository.class);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, methodProxy) -> {
            long start = System.nanoTime();
            Object result = methodProxy.invokeSuper(obj, args);
            System.out.printf("%s() выполнен за %d нс%n", method.getName(), System.nanoTime() - start);
            return result;
        });
        return (InstrumentRepository) enhancer.create();
    }
}
