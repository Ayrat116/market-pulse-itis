package academy.backend.market_pulse.demo;


public class SneakyThrowsDemo {

    static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        //noinspection unchecked
        throw (E) e;
    }

    public static void main(String[] args) {
        // throw new Exception("Test"); // не скомпилируется, так как требует укащания в сигнатуре
        sneakyThrow(new Exception("Test"));
    }
}

