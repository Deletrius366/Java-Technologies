package ru.ifmo.rain.levashov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {

    private ParallelMapper mapper;

    public IterativeParallelism() {
        mapper = null;
    }

    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }


    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return working(threads, values, s -> s.map(Object::toString).collect(Collectors.joining()),
                s -> s.collect(Collectors.joining()));
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    private void joinThreads(final List<Thread> workers) throws InterruptedException {
        for (int i = 0; i < workers.size(); i++) {
            try {
                workers.get(i).join();
            } catch (final InterruptedException e) {
                final InterruptedException interruptedException = new InterruptedException("Failed to join some threads");
                interruptedException.addSuppressed(e);
                for (int j = i; j < workers.size(); j++) {
                    workers.get(j).interrupt();
                }
                for (int j = i; j < workers.size(); j++) {
                    try {
                        workers.get(j).join();
                    } catch (final InterruptedException e1) {
                        interruptedException.addSuppressed(e1);
                        j--;
                    }
                }
                throw interruptedException;
            }
        }
    }

    private <T, U, R> R working(final int threads, final List<T> values,
                                final Function<Stream<T>, U> functionTo,
                                final Function<Stream<U>, R> functionFrom)
            throws InterruptedException {
        final List<Stream<T>> newList = split(threads, values);
        final List<Thread> workers = new ArrayList<>();
        final List<U> result;
        if (mapper == null) {
            result = new ArrayList<>(Collections.nCopies(newList.size(), null));
            for (int i = 0; i < newList.size(); ++i) {
                final int nextIndex = i;
                final Thread thread = new Thread(() -> result.set(nextIndex, functionTo.apply(newList.get(nextIndex))));
                workers.add(thread);
                thread.start();
            }
            // :NOTE(solved): .join должен быть для всех потоков
            joinThreads(workers);
        } else {
            result = mapper.map(functionTo, newList);
        }
        return functionFrom.apply(result.stream());
    }

    private <T> List<T> mapFilterReduce(final Stream<? extends List<? extends T>> streams) {
        return streams.flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        // :NOTE(solved): унифицировать c map
        return working(threads, values, s -> s.filter(predicate).collect(Collectors.toList()), this::mapFilterReduce);
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f)
            throws InterruptedException {
        return working(threads, values, s -> s.map(f).collect(Collectors.toList()), this::mapFilterReduce);
    }

    private <T> T doMaximum(final int threads, final List<T> values, final Function<Stream<T>, T> comparator) throws InterruptedException {
        return working(threads, values, comparator, comparator);
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        // :NOTE(solved): копипаста
        return doMaximum(threads, values, s -> s.max(comparator).get());
    }

    private <T> List<Stream<T>> split(final int threads, final List<T> values) {
        final int amountPerThreadDivided = values.size() / threads;
        final int additional = values.size() % threads;
        final List<Stream<T>> streams = new ArrayList<>();
        int processed = 0;
        for (int i = 0; i < threads; ++i) {
            int amountPerThread = amountPerThreadDivided;
            if (i < additional) {
                amountPerThread++;
            }
            if (amountPerThread > 0) {
                streams.add(values.subList(processed, processed + amountPerThread).stream());
            }
            processed += amountPerThread;
        }
        return streams;
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return working(threads, values, s -> s.allMatch(predicate), s -> s.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        // :NOTE(solved): унифицировать с any
        return !all(threads, values, predicate.negate());
    }

    private <T> T applyReduce(final Stream<T> stream, final Monoid<T> monoid) {
        return stream.reduce(monoid.getIdentity(), monoid.getOperator());
    }

    private <T, R> R applyMapReduce(final Stream<T> stream, final Monoid<R> monoid, final Function<T, R> lift) {
        return applyReduce(stream.map(lift), monoid);
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return working(threads, values, s -> applyReduce(s, monoid), s -> applyReduce(s, monoid));
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid)
            throws InterruptedException {
        // :NOTE(solved): копипаста
        return working(threads, values, s -> applyMapReduce(s, monoid, lift), s -> applyReduce(s, monoid));
    }
}