package ru.ifmo.rain.levashov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.ifmo.rain.levashov.hello.HelloUDPServer.serverMain;
import static ru.ifmo.rain.levashov.hello.Utils.*;

public class HelloUDPNonblockingServer implements HelloServer {

    private static byte[] PREFIX = "Hello, ".getBytes();

    private class Pair {
        SocketAddress socketAddress;
        ByteBuffer buffer;

        Pair(SocketAddress socketAddress, ByteBuffer buffer) {
            this.socketAddress = socketAddress;
            this.buffer = buffer;
        }
    }

    private ExecutorService workers;
    private ExecutorService mainWorker;
    private Selector selector;
    private DatagramChannel channel;
    private int bufferSize;
    private int threads;

    public static void main(String []args) {
        serverMain(new HelloUDPNonblockingServer(), args);
    }

    private void mainWork() {
        final Deque<Pair> emptyBuffers = new ArrayDeque<>(threads);
        for (int i = 0; i < threads; i++) {
            emptyBuffers.add(new Pair(null, ByteBuffer.allocate(bufferSize)));
        }
        final Deque<Pair> buffers = new ArrayDeque<>(threads);
        try {
            while (!Thread.interrupted() && !channel.socket().isClosed()) {
                selector.select();
                final Iterator<SelectionKey> i = selector.selectedKeys().iterator();
                if (i.hasNext()) {
                    final SelectionKey key = i.next();
                    if (key.isReadable()) {
                        Pair request = emptyBuffers.remove();
                        ByteBuffer buffer = request.buffer.clear();
                        if (emptyBuffers.isEmpty()) {
                            key.interestOpsAnd(~SelectionKey.OP_READ);
                        }
                        try {
                            request.socketAddress = channel.receive(buffer);
                        } catch (IOException e) {
                            System.err.println("Failed to receive data: " + e.getMessage());
                            continue;
                        }

                        workers.submit(() -> {
                            buffer.flip();
                            final byte[] bufferArray = buffer.array();
                            System.arraycopy(bufferArray, buffer.arrayOffset(), bufferArray, PREFIX.length, buffer.limit());
                            System.arraycopy(PREFIX, 0, bufferArray, 0, PREFIX.length);
                            buffer.limit(buffer.limit() + PREFIX.length);
                            synchronized (buffers) {
                                buffers.add(request);
                                if (buffers.size() == 1) {
                                    key.interestOpsOr(SelectionKey.OP_WRITE);
                                    selector.wakeup();
                                }
                            }
                        });
                    }
                    if (key.isWritable()) {
                        Pair request;
                        synchronized (buffers) {
                            request = buffers.remove();
                        }
                        if (buffers.isEmpty()) {
                            key.interestOpsAnd(~SelectionKey.OP_WRITE);
                        }

                        try {
                            channel.send(request.buffer, request.socketAddress);
                        } catch (IOException e) {
                            System.err.println("Failed to receive data: " + e.getMessage());
                            continue;
                        }
                        emptyBuffers.add(request);

                        key.interestOpsOr(SelectionKey.OP_READ);
                    }
                    i.remove();
                }
            }
        } catch (IOException e) {
            System.err.println("Selection failed: " + e.getMessage());
        }
    }

    @Override
    public void start(final int port, final int threads) {
        workers = Executors.newFixedThreadPool(threads);
        mainWorker = Executors.newSingleThreadExecutor();
        this.threads = threads;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.err.println("Failed to open selector: " + e.getMessage());
        }
        try {
            channel = DatagramChannel.open();
        } catch (IOException e) {
            System.err.println("Failed to open channel: " + e.getMessage());
            safeClose(selector);
            return;
        }
        try {
            bufferSize = channel.socket().getReceiveBufferSize();
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("Error occurred during channel configuration: " + e.getMessage());
            safeClose(selector);
        }
        mainWorker.submit(this::mainWork);
    }

    @Override
    public void close() {
        try {
            selector.close();
            channel.close();
        } catch (IOException e) {
            System.err.println("Error while selector or channel was closing " + e.getMessage());
        }
        workers.shutdown();
        mainWorker.shutdown();
        try {
            workers.awaitTermination(1, TimeUnit.SECONDS);
            mainWorker.awaitTermination(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            System.err.println("Failed to terminate threads: " + e.getMessage());
        }
    }
}
