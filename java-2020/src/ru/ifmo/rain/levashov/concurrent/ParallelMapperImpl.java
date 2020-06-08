package ru.ifmo.rain.levashov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> workers;
    private ParallelQueue tasks;

    private class ParallelQueue {
        private final Queue<Task<?, ?>> tasks;

        ParallelQueue() {
            tasks = new ArrayDeque<>();
        }

        private synchronized void addTask(final Task<?, ?> task) {
            tasks.add(task);
            notifyAll();
        }

        private synchronized Runnable doTask() throws InterruptedException {
            while (tasks.isEmpty()) {
                wait();
            }
            return tasks.element().getSubtask();
        }

        private synchronized void removeTask() {
            tasks.remove();
        }

        private synchronized void terminateAll() {
            tasks.forEach(Task::taskFinish);
        }
    }

    public ParallelMapperImpl(final int threads) {
        tasks = new ParallelQueue();
        final Runnable task = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    tasks.doTask().run();
                }
            } catch (final InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        });

        workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(task));
        }
        workers.forEach(Thread::start);
    }

    private class Task<T, R> {
        private final Queue<Runnable> subtasks;
        private final List<R> collector;
        private RuntimeException runtimeExceptions;
        private int notStarted, notFinish;
        private boolean mustBeFinished;

        Task(final Function<? super T, ? extends R> f, final List<? extends T> args) {
            collector = new ArrayList<>(Collections.nCopies(args.size(), null));
            runtimeExceptions = null;
            subtasks = new ArrayDeque<>();
            notFinish = notStarted = args.size();

            IntStream.range(0, args.size())
                    .forEach(i -> subtasks.add(() -> {
                        try {
                            set(i, f.apply(args.get(i)));
                        } catch (RuntimeException e) {
                            addException(e);
                        }
                    }));
        }

        private synchronized Runnable getSubtask() {
            Runnable subtask = subtasks.poll();
            if (--notStarted == 0) {
                tasks.removeTask();
            }

            return subtask;
        }

        public synchronized void set(final int index, final R object) {
            collector.set(index, object);
            subtaskFinish();
        }

        synchronized void taskFinish() {
            mustBeFinished = true;
            notify();
        }

        private synchronized void subtaskFinish() {
            if (--notFinish == 0) {
                taskFinish();
            }
        }

        synchronized void addException(final RuntimeException exception) {
            if (runtimeExceptions == null) {
                runtimeExceptions = exception;
            } else {
                runtimeExceptions.addSuppressed(exception);
            }
            subtaskFinish();
        }

        synchronized void waitFinish() throws InterruptedException {
            while (!mustBeFinished) {
                wait();
            }
        }

        synchronized List<R> getResult() throws InterruptedException {
            waitFinish();

            if (runtimeExceptions != null) {
                throw runtimeExceptions;
            }
            return collector;
        }

    }


    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        Task<T, R> task;

        synchronized (this) {
            task = new Task<>(f, args);
            tasks.addTask(task);
        }

        return task.getResult();
    }


    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        tasks.terminateAll();
        workers.forEach(thread ->
        {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException ignored) {
                }
            }
        });
    }
}
