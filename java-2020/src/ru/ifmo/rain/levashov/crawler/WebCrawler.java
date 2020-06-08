package ru.ifmo.rain.levashov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService downloadPool;
    private final ExecutorService linksExtractPool;
    private final ConcurrentMap<String, HostDownloader> hostDownloaders;

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;

        downloadPool = Executors.newFixedThreadPool(downloaders);
        linksExtractPool = Executors.newFixedThreadPool(extractors);
        hostDownloaders = new ConcurrentHashMap<>();
    }

    private class HostDownloader {

        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private int now = 0;

        private synchronized void runTask() {
            if (now < perHost) {
                final Runnable task = tasks.poll();
                if (task != null) {
                    now++;
                    downloadPool.submit(() -> {
                        try {
                            task.run();
                        } finally {
                            synchronized (HostDownloader.this) {
                                now--;
                                runTask();
                            }
                        }
                    });
                }
            }

        }

        private synchronized void addTask(final Runnable task) {
            tasks.add(task);
            runTask();
        }
    }

    private class Worker {
        private final Phaser phaser = new Phaser(1);

        private final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        private final Set<String> extracted = ConcurrentHashMap.newKeySet();
        private final Map<String, IOException> errors = new ConcurrentHashMap<>();

        Worker(final String url, final int depth) {
            extracted.add(url);
            levelDownload(url, depth);
            phaser.arriveAndAwaitAdvance();
        }

        private void levelDownload(final String link, final int level) {

            final String host;
            try {
                host = URLUtils.getHost(link);
            } catch (final MalformedURLException e) {
                errors.put(link, e);
                return;
            }

            final HostDownloader hostDownloader = hostDownloaders.computeIfAbsent(host, s -> new HostDownloader());
            phaser.register();
            hostDownloader.addTask(() -> {
                try {
                    final Document document = downloader.download(link);
                    downloaded.add(link);

                    if (level != 1) {
                        levelLinksExtraction(document, level, phaser);
                    }
                } catch (final IOException e) {
                    errors.put(link, e);
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }

        private void levelLinksExtraction(final Document document, final int level, final Phaser levelPhaser) {
            levelPhaser.register();
            linksExtractPool.submit(() -> {
                try {
                    document.extractLinks().stream()
                            .filter(extracted::add)
                            .forEach(link -> levelDownload(link, level - 1));
                } catch (final IOException ignored) {
                } finally {
                    levelPhaser.arriveAndDeregister();
                }
            });
        }

        private Result getResult() {
            phaser.arriveAndAwaitAdvance();
            return new Result(new ArrayList<>(downloaded), errors);
        }

    }

    @Override
    public Result download(final String url, final int depth) {
        return new Worker(url, depth).getResult();
    }

    @Override
    public void close() {
        linksExtractPool.shutdown();
        downloadPool.shutdown();

        try {
            linksExtractPool.awaitTermination(0, TimeUnit.SECONDS);
            downloadPool.awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Failed to terminate thread pools: " + e.getMessage());
        }
    }

    private static int getArgumentOrDefault(final String[] args, final int index) {
        return index < args.length ? Integer.parseInt(args[index]) : 1;
    }

    public static void main(final String[] args) {
        if (args == null || args.length == 0 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("All arguments must be not null and at least one argument must present");
            return;
        }
        try {
            final int depth = getArgumentOrDefault(args, 1);
            final int downloaders = getArgumentOrDefault(args, 2);
            final int extractors = getArgumentOrDefault(args, 3);
            final int perHost = getArgumentOrDefault(args, 4);

            try (final Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                crawler.download(args[0], depth);
            }
        } catch (final NumberFormatException e) {
            System.err.println("All arguments except first must be numbers");
        } catch (final IOException e) {
            System.err.println("Failed to initialize downloader: " + e.getMessage());
        }
    }

}
