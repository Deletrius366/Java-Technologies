package ru.ifmo.rain.levashov.bank.tests;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.*;

public class BaseTest {

    public <E extends Exception> void parallelCommands(final int threads, final List<Command<E>> commands) {
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (final Future<Void> future : executor.invokeAll(commands)) {
                future.get();
            }
            executor.shutdown();
        } catch (final InterruptedException | ExecutionException e) {
            throw new AssertionError(e);
        }
    }

    public <E extends Exception> void parallel(final int threads, final Command<E> command) {
        parallelCommands(threads, Collections.nCopies(threads, command));
    }

    public interface Command<E extends Exception> extends Callable<Void> {
        @Override
        default Void call() throws E {
            run();
            return null;
        }

        void run() throws E;
    }
}
